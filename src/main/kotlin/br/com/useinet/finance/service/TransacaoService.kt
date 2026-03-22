package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ImportResultResponse
import br.com.useinet.finance.dto.PageResponse
import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class TransacaoService(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val orcamentoService: OrcamentoService
) {

    @Transactional
    fun criar(request: TransacaoRequest, usuario: Usuario): TransacaoResponse {
        validarRequest(request)
        val transacao = Transacao().apply {
            this.descricao = request.descricao
            this.valor = request.valor
            this.tipo = request.tipo
            this.data = request.data ?: LocalDateTime.now()
            this.usuario = usuario
        }

        if (request.categoriaId != null) {
            transacao.categoria = categoriaRepository.findById(request.categoriaId)
                .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        }

        if (request.contaId != null) {
            val conta = contaRepository.findByIdAndUsuario(request.contaId, usuario)
                .orElseThrow { IllegalArgumentException("Conta não encontrada.") }
            transacao.conta = conta
            ajustarSaldo(conta, request.tipo!!, request.valor!!)
            contaRepository.save(conta)
        }

        val saved = transacaoRepository.save(transacao)
        if (transacao.tipo == TipoTransacao.DESPESA) {
            val dt = transacao.data!!
            orcamentoService.checkAlerts(usuario, dt.monthValue, dt.year)
        }
        return TransacaoResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun listar(usuario: Usuario, inicio: LocalDateTime?, fim: LocalDateTime?, pageable: Pageable): PageResponse<TransacaoResponse> {
        val page = if (inicio != null && fim != null)
            transacaoRepository.findByUsuarioAndDataBetween(usuario, inicio, fim, pageable)
        else
            transacaoRepository.findByUsuario(usuario, pageable)
        return PageResponse(
            content = page.content.map { TransacaoResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

    @Transactional
    fun atualizar(id: Long, request: TransacaoRequest, usuario: Usuario): TransacaoResponse {
        validarRequest(request)
        val tipo = request.tipo!!
        val valor = request.valor!!
        val transacao = transacaoRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Transação não encontrada.") }

        // Reverter efeito na conta anterior
        transacao.conta?.let {
            reverterSaldo(it, requireNotNull(transacao.tipo), requireNotNull(transacao.valor))
            contaRepository.save(it)
        }

        transacao.descricao = request.descricao
        transacao.valor = valor
        transacao.tipo = tipo
        if (request.data != null) transacao.data = request.data

        transacao.categoria = if (request.categoriaId != null) {
            categoriaRepository.findById(request.categoriaId)
                .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        } else null

        // Aplicar efeito na nova conta
        val contaId = request.contaId
        if (contaId != null) {
            val conta = contaRepository.findByIdAndUsuario(contaId, usuario)
                .orElseThrow { IllegalArgumentException("Conta não encontrada.") }
            transacao.conta = conta
            ajustarSaldo(conta, tipo, valor)
            contaRepository.save(conta)
        } else {
            transacao.conta = null
        }

        return TransacaoResponse.from(transacaoRepository.save(transacao))
    }

    @Transactional
    fun deletar(id: Long, usuario: Usuario) {
        val transacao = transacaoRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Transação não encontrada.") }
        transacao.conta?.let {
            reverterSaldo(it, requireNotNull(transacao.tipo), requireNotNull(transacao.valor))
            contaRepository.save(it)
        }
        transacaoRepository.delete(transacao)
    }

    private fun validarRequest(request: TransacaoRequest) {
        if (request.descricao.isNullOrBlank()) throw IllegalArgumentException("Descrição da transação é obrigatória.")
        if (request.valor == null || request.valor <= 0) throw IllegalArgumentException("Valor da transação deve ser maior que zero.")
        if (request.tipo == null) throw IllegalArgumentException("Tipo da transação é obrigatório.")
    }

    private fun ajustarSaldo(conta: Conta, tipo: TipoTransacao, valor: Double) {
        val saldo = requireNotNull(conta.saldo) { "Conta.saldo não pode ser nulo" }
        conta.saldo = if (tipo == TipoTransacao.RECEITA) saldo + valor else saldo - valor
    }

    private fun reverterSaldo(conta: Conta, tipo: TipoTransacao, valor: Double) {
        val saldo = requireNotNull(conta.saldo) { "Conta.saldo não pode ser nulo" }
        conta.saldo = if (tipo == TipoTransacao.RECEITA) saldo - valor else saldo + valor
    }

    @Transactional(readOnly = true)
    fun exportarCsv(usuario: Usuario, inicio: LocalDateTime?, fim: LocalDateTime?): ByteArray {
        val fmtDateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val transacoes = if (inicio != null && fim != null)
            transacaoRepository.findByUsuarioAndDataBetweenOrderByDataAscIdAsc(usuario, inicio, fim)
        else
            transacaoRepository.findByUsuarioOrderByDataAscIdAsc(usuario)

        val totalReceitas = transacoes.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor ?: 0.0 }
        val totalDespesas = transacoes.filter { it.tipo == TipoTransacao.DESPESA }.sumOf { it.valor ?: 0.0 }
        val saldo = totalReceitas - totalDespesas
        val periodo = if (inicio != null && fim != null)
            "${inicio.format(fmtDate)} a ${fim.format(fmtDate)}"
        else "Todas as transações"

        fun fmtDecimal(v: Double) = "%.2f".format(v).replace(".", ",")

        val csv = StringBuilder()
        csv.append('\uFEFF') // BOM para compatibilidade com Excel

        // Resumo
        csv.append("Período:;${escapeCsvBr(periodo)}\n")
        csv.append("Total Receitas:;${fmtDecimal(totalReceitas)}\n")
        csv.append("Total Despesas:;${fmtDecimal(totalDespesas)}\n")
        csv.append("Resultado do período:;${fmtDecimal(saldo)}\n")
        csv.append("\n")

        // Pré-calcula saldo acumulado após cada transação por conta (extrato)
        // Busca TODAS as transações de cada conta para calcular saldo correto mesmo com filtro de período
        val saldoAposTransacao: Map<Long, Double> = transacoes
            .mapNotNull { it.conta }
            .distinctBy { it.id }
            .flatMap { conta ->
                var saldo = 0.0
                transacaoRepository.findByContaOrderByDataAscIdAsc(conta).map { t ->
                    saldo += if (t.tipo == TipoTransacao.RECEITA) t.valor!! else -(t.valor!!)
                    t.id!! to saldo
                }
            }
            .toMap()

        // Cabeçalho
        csv.append("ID;Descrição;Valor;Tipo;Data;Categoria;Conta;\"Saldo da Conta\"\n")

        // Linhas (mais antigas primeiro — ASC, como extrato bancário)
        transacoes.forEach { t ->
            val tipoLegivel = if (t.tipo == TipoTransacao.RECEITA) "Receita" else "Despesa"
            csv.append(t.id).append(';')
                .append(escapeCsvBr(t.descricao)).append(';')
                .append(fmtDecimal(t.valor ?: 0.0)).append(';')
                .append(tipoLegivel).append(';')
                .append(escapeCsvBr(requireNotNull(t.data).format(fmtDateTime))).append(';')
                .append(t.categoria?.let { escapeCsvBr(it.nome) } ?: "").append(';')
                .append(t.conta?.let { escapeCsvBr(it.nome) } ?: "").append(';')
                .append(t.id?.let { saldoAposTransacao[it]?.let { v -> fmtDecimal(v) } } ?: "")
                .append('\n')
        }

        return csv.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @Transactional
    fun importarCsv(file: MultipartFile, usuario: Usuario): ImportResultResponse {
        var content = file.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        // Strip UTF-8 BOM if present
        if (content.startsWith("\uFEFF")) content = content.substring(1)

        // Find the data header line — skip summary lines produced by exportarCsv
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { line ->
            val lower = line.lowercase()
            lower.startsWith("id,") || lower.startsWith("id;") ||
                lower.contains("descrição") || lower.contains("descricao")
        }
        val effectiveHeaderIndex = if (headerIndex >= 0) headerIndex else 0
        // Detect separator and parse column names from the actual header line
        val headerLine = lines[effectiveHeaderIndex].trimStart('\uFEFF')
        val separator = if (headerLine.contains(';')) ';' else ','
        val explicitHeaders = headerLine.split(separator).map { it.trim().trim('"') }
        val csvContent = lines.drop(effectiveHeaderIndex + 1).joinToString("\n")

        val fmtExport = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(separator)
            .setHeader(*explicitHeaders.toTypedArray())
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()

        val records = CSVParser.parse(csvContent, csvFormat)

        // Resolve column names flexibly — returns the ACTUAL header string from the file
        fun colName(vararg candidates: String): String? =
            candidates.firstNotNullOfOrNull { c -> explicitHeaders.firstOrNull { it.equals(c, ignoreCase = true) } }

        val colDescricao = colName("Descrição", "Descricao", "descricao") ?: "Descrição"
        val colValor     = colName("Valor", "valor") ?: "Valor"
        val colTipo      = colName("Tipo", "tipo") ?: "Tipo"
        val colData      = colName("Data", "data") ?: "Data"
        val colCategoria = colName("Categoria", "categoria") ?: "Categoria"
        val colConta     = colName("Conta", "conta")

        var importadas = 0
        var duplicatas = 0
        val erros = mutableListOf<String>()
        var rowIndex = 0

        for (row in records) {
            rowIndex++
            val linha = effectiveHeaderIndex + 1 + rowIndex
            try {
                val descricao = row.get(colDescricao)
                val valorRaw = row.get(colValor).replace(",", ".").toDouble()
                val valor = abs(valorRaw)
                val tipoStr = row.get(colTipo).trim()
                val tipo = when (tipoStr.uppercase()) {
                    "RECEITA", "REVENUE", "INCOME" -> TipoTransacao.RECEITA
                    "DESPESA", "EXPENSE"            -> TipoTransacao.DESPESA
                    else -> TipoTransacao.valueOf(tipoStr.uppercase())
                }
                val dataStr = row.get(colData).trim()
                val data: LocalDateTime = try {
                    LocalDateTime.parse(dataStr, fmtExport)
                } catch (_: Exception) {
                    try {
                        LocalDate.parse(dataStr).atStartOfDay()
                    } catch (_: Exception) {
                        LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay()
                    }
                }

                if (transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(usuario, data, valor, descricao)) {
                    duplicatas++
                    continue
                }

                val transacao = Transacao().apply {
                    this.descricao = descricao
                    this.valor = valor
                    this.tipo = tipo
                    this.data = data
                    this.usuario = usuario
                }

                val categoriaStr = try { row.get(colCategoria) } catch (_: Exception) { "" }
                if (categoriaStr.isNotBlank()) {
                    categoriaRepository.findByNome(categoriaStr).ifPresent { transacao.categoria = it }
                }

                if (colConta != null) {
                    val contaStr = try { row.get(colConta) } catch (_: Exception) { "" }
                    if (contaStr.isNotBlank()) {
                        contaRepository.findByNomeAndUsuario(contaStr, usuario).ifPresent { conta ->
                            transacao.conta = conta
                            ajustarSaldo(conta, tipo, valor)
                            contaRepository.save(conta)
                        }
                    }
                }

                transacaoRepository.save(transacao)
                importadas++
            } catch (e: Exception) {
                erros.add("Linha $linha: ${e.message}")
            }
        }

        return ImportResultResponse(importadas, duplicatas, erros.size, erros)
    }

    private fun escapeCsvBr(value: String?): String {
        if (value == null) return ""
        return if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains(" "))
            "\"${value.replace("\"", "\"\"")}\""
        else value
    }
}
