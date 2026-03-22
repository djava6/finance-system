package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ImportResultResponse
import br.com.useinet.finance.dto.PageResponse
import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.FrequenciaRecorrencia
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.dhatim.fastexcel.Workbook
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class TransacaoService(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val orcamentoService: OrcamentoService
) {
    private val log = LoggerFactory.getLogger(TransacaoService::class.java)

    @Transactional
    fun criar(request: TransacaoRequest, usuario: Usuario): TransacaoResponse {
        validarRequest(request)
        if (request.recorrente && request.frequencia == null) {
            throw IllegalArgumentException("Frequência é obrigatória para transações recorrentes.")
        }
        val transacao = Transacao().apply {
            this.descricao = request.descricao
            this.valor = request.valor
            this.tipo = request.tipo
            this.data = request.data ?: LocalDate.now()
            this.usuario = usuario
            this.recorrente = request.recorrente
            this.frequencia = request.frequencia
            this.proximaOcorrencia = if (request.recorrente) {
                request.proximaOcorrencia ?: calcularProximaOcorrencia(
                    request.data ?: LocalDate.now(), request.frequencia!!
                )
            } else null
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
    fun listar(usuario: Usuario, inicio: LocalDate?, fim: LocalDate?, pageable: Pageable): PageResponse<TransacaoResponse> {
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
    fun exportarCsv(usuario: Usuario, inicio: LocalDate?, fim: LocalDate?): ByteArray {
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
        csv.append("ID;Data;Descrição;Tipo;Categoria;Conta;Valor;Saldo da Conta\n")

        // Linhas (mais antigas primeiro — ASC, como extrato bancário)
        transacoes.forEach { t ->
            val tipoLegivel = if (t.tipo == TipoTransacao.RECEITA) "Receita" else "Despesa"
            csv.append(t.id).append(';')
                .append(escapeCsvBr(requireNotNull(t.data).format(fmtDate))).append(';')
                .append(escapeCsvBr(t.descricao)).append(';')
                .append(tipoLegivel).append(';')
                .append(t.categoria?.let { escapeCsvBr(it.nome) } ?: "").append(';')
                .append(t.conta?.let { escapeCsvBr(it.nome) } ?: "").append(';')
                .append(fmtDecimal(t.valor ?: 0.0)).append(';')
                .append(t.id?.let { saldoAposTransacao[it]?.let { v -> fmtDecimal(v) } } ?: "")
                .append('\n')
        }

        return csv.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @Transactional(readOnly = true)
    fun exportarXlsx(usuario: Usuario, inicio: LocalDate?, fim: LocalDate?, outputStream: java.io.OutputStream) {
        val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val transacoes = if (inicio != null && fim != null)
            transacaoRepository.findByUsuarioAndDataBetweenOrderByDataAscIdAsc(usuario, inicio, fim)
        else
            transacaoRepository.findByUsuarioOrderByDataAscIdAsc(usuario)

        val totalReceitas = transacoes.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor ?: 0.0 }
        val totalDespesas = transacoes.filter { it.tipo == TipoTransacao.DESPESA }.sumOf { it.valor ?: 0.0 }
        val resultado = totalReceitas - totalDespesas
        val periodo = if (inicio != null && fim != null)
            "${inicio.format(fmtDate)} a ${fim.format(fmtDate)}"
        else "Todas as transações"

        // Saldo acumulado por conta — mesma lógica do CSV
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

        val BLUE  = "E3F2FD"
        val GRAY  = "EEEEEE"
        val GREEN = "E8F5E9"
        val RED   = "FFEBEE"
        val CURRENCY = "#,##0.00"
        val DATE_FMT = "dd/mm/yyyy"

        Workbook(outputStream, "Finance System", "1.0").use { wb ->

            // ── Aba 1: Transações ──────────────────────────────────────────
            val ws = wb.newWorksheet("Transações")

            ws.width(0, 8.0)   // ID
            ws.width(1, 14.0)  // Data
            ws.width(2, 38.0)  // Descrição
            ws.width(3, 12.0)  // Tipo
            ws.width(4, 22.0)  // Categoria
            ws.width(5, 22.0)  // Conta
            ws.width(6, 15.0)  // Valor
            ws.width(7, 18.0)  // Saldo da Conta

            // Bloco de resumo (linhas 0–3)
            listOf(
                "Período:" to periodo,
                "Total Receitas:" to totalReceitas,
                "Total Despesas:" to totalDespesas,
                "Resultado do período:" to resultado
            ).forEachIndexed { row, (label, value) ->
                ws.value(row, 0, label)
                ws.style(row, 0).fillColor(BLUE).bold().set()
                if (value is String) {
                    ws.value(row, 1, value)
                    ws.style(row, 1).fillColor(BLUE).set()
                } else {
                    ws.value(row, 1, (value as Double))
                    ws.style(row, 1).fillColor(BLUE).format(CURRENCY).set()
                }
            }

            // Linha 4: separador em branco
            // Linha 5: cabeçalho
            val HEADER_ROW = 5
            listOf("ID", "Data", "Descrição", "Tipo", "Categoria", "Conta", "Valor", "Saldo da Conta")
                .forEachIndexed { col, h ->
                    ws.value(HEADER_ROW, col, h)
                    ws.style(HEADER_ROW, col).fillColor(GRAY).bold().set()
                }

            // Linhas de dados (a partir da linha 6)
            transacoes.forEachIndexed { i, t ->
                val row = HEADER_ROW + 1 + i
                val color = if (t.tipo == TipoTransacao.RECEITA) GREEN else RED

                ws.value(row, 0, t.id)
                ws.style(row, 0).fillColor(color).set()

                ws.value(row, 1, t.data)
                ws.style(row, 1).fillColor(color).format(DATE_FMT).set()

                ws.value(row, 2, t.descricao)
                ws.style(row, 2).fillColor(color).set()

                ws.value(row, 3, if (t.tipo == TipoTransacao.RECEITA) "Receita" else "Despesa")
                ws.style(row, 3).fillColor(color).set()

                ws.value(row, 4, t.categoria?.nome ?: "")
                ws.style(row, 4).fillColor(color).set()

                ws.value(row, 5, t.conta?.nome ?: "")
                ws.style(row, 5).fillColor(color).set()

                ws.value(row, 6, t.valor)
                ws.style(row, 6).fillColor(color).format(CURRENCY).set()

                val saldo = t.id?.let { saldoAposTransacao[it] }
                if (saldo != null) {
                    ws.value(row, 7, saldo)
                    ws.style(row, 7).fillColor(color).format(CURRENCY).set()
                } else {
                    ws.style(row, 7).fillColor(color).set()
                }
            }

            // Linha de totais
            if (transacoes.isNotEmpty()) {
                val totalsRow = HEADER_ROW + 1 + transacoes.size
                ws.value(totalsRow, 0, "TOTAL")
                for (col in 0..5) ws.style(totalsRow, col).fillColor(GRAY).bold().set()
                ws.value(totalsRow, 6, resultado)
                ws.style(totalsRow, 6).fillColor(GRAY).bold().format(CURRENCY).set()
                ws.style(totalsRow, 7).fillColor(GRAY).bold().set()
            }

            // ── Aba 2: Resumo por Categoria ────────────────────────────────
            val wsRes = wb.newWorksheet("Resumo")

            wsRes.width(0, 28.0)
            wsRes.width(1, 16.0)
            wsRes.width(2, 16.0)

            listOf("Categoria", "Despesas (R$)", "Receitas (R$)").forEachIndexed { col, h ->
                wsRes.value(0, col, h)
                wsRes.style(0, col).fillColor(GRAY).bold().set()
            }

            val porCategoria = transacoes
                .groupBy { it.categoria?.nome ?: "Sem categoria" }
                .entries.sortedBy { it.key }

            porCategoria.forEachIndexed { i, (cat, trans) ->
                val row = i + 1
                val desp = trans.filter { it.tipo == TipoTransacao.DESPESA }.sumOf { it.valor ?: 0.0 }
                val rec  = trans.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor ?: 0.0 }
                wsRes.value(row, 0, cat)
                wsRes.value(row, 1, desp)
                wsRes.style(row, 1).fillColor(RED).format(CURRENCY).set()
                wsRes.value(row, 2, rec)
                wsRes.style(row, 2).fillColor(GREEN).format(CURRENCY).set()
            }

            val totResRow = porCategoria.size + 1
            wsRes.value(totResRow, 0, "TOTAL")
            wsRes.style(totResRow, 0).fillColor(GRAY).bold().set()
            wsRes.value(totResRow, 1, totalDespesas)
            wsRes.style(totResRow, 1).fillColor(GRAY).bold().format(CURRENCY).set()
            wsRes.value(totResRow, 2, totalReceitas)
            wsRes.style(totResRow, 2).fillColor(GRAY).bold().format(CURRENCY).set()
        }
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

        val fmtDMY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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
                val data: LocalDate = try {
                    LocalDate.parse(dataStr, fmtDMY)  // dd/MM/yyyy (export format)
                } catch (_: Exception) {
                    try {
                        LocalDate.parse(dataStr)  // yyyy-MM-dd (ISO, simple import)
                    } catch (_: Exception) {
                        // backward compat: old exports had dd/MM/yyyy HH:mm — use date part only
                        LocalDate.parse(dataStr.take(10), fmtDMY)
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

    private fun calcularProximaOcorrencia(base: LocalDate, frequencia: FrequenciaRecorrencia): LocalDate =
        when (frequencia) {
            FrequenciaRecorrencia.SEMANAL -> base.plusWeeks(1)
            FrequenciaRecorrencia.MENSAL  -> base.plusMonths(1)
            FrequenciaRecorrencia.ANUAL   -> base.plusYears(1)
        }

    @Scheduled(cron = "0 0 6 * * *") // todo dia às 06:00
    @Transactional
    fun processarRecorrencias() {
        val hoje = LocalDate.now()
        val pendentes = transacaoRepository.findByRecorrenteIsTrueAndProximaOcorrenciaLessThanEqual(hoje)
        if (pendentes.isEmpty()) return

        log.info("Processando ${pendentes.size} transação(ões) recorrente(s)")
        for (template in pendentes) {
            val nova = Transacao().apply {
                descricao = template.descricao
                valor = template.valor
                tipo = template.tipo
                data = template.proximaOcorrencia!!
                usuario = template.usuario
                categoria = template.categoria
                conta = template.conta
                recorrente = false
            }
            transacaoRepository.save(nova)

            template.conta?.let {
                ajustarSaldo(it, requireNotNull(template.tipo), requireNotNull(template.valor))
                contaRepository.save(it)
            }

            template.proximaOcorrencia = calcularProximaOcorrencia(
                template.proximaOcorrencia!!, requireNotNull(template.frequencia)
            )
            transacaoRepository.save(template)
        }
        log.info("Recorrências processadas: ${pendentes.size}")
    }

    private fun escapeCsvBr(value: String?): String {
        if (value == null) return ""
        return if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains(" "))
            "\"${value.replace("\"", "\"\"")}\""
        else value
    }
}
