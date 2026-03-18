package br.com.useinet.finance.service

import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TransacaoService(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository
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

        return TransacaoResponse.from(transacaoRepository.save(transacao))
    }

    @Transactional(readOnly = true)
    fun listar(usuario: Usuario, inicio: LocalDateTime?, fim: LocalDateTime?): List<TransacaoResponse> {
        val transacoes = if (inicio != null && fim != null)
            transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(usuario, inicio, fim)
        else
            transacaoRepository.findByUsuarioOrderByDataDesc(usuario)
        return transacoes.map { TransacaoResponse.from(it) }
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
            transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(usuario, inicio, fim)
        else
            transacaoRepository.findByUsuarioOrderByDataDesc(usuario)

        val totalReceitas = transacoes.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor ?: 0.0 }
        val totalDespesas = transacoes.filter { it.tipo == TipoTransacao.DESPESA }.sumOf { it.valor ?: 0.0 }
        val saldo = totalReceitas - totalDespesas
        val periodo = if (inicio != null && fim != null)
            "${inicio.format(fmtDate)} a ${fim.format(fmtDate)}"
        else "Todas as transações"

        val csv = StringBuilder()
        csv.append('\uFEFF') // BOM para compatibilidade com Excel

        // Resumo
        csv.append("Período:,${escapeCsv(periodo)}\n")
        csv.append("Total Receitas:,\"${"%.2f".format(totalReceitas)}\"\n")
        csv.append("Total Despesas:,\"${"%.2f".format(totalDespesas)}\"\n")
        csv.append("Saldo:,\"${"%.2f".format(saldo)}\"\n")
        csv.append("\n")

        // Cabeçalho
        csv.append("ID,Descrição,Valor,Tipo,Data,Categoria,Conta,Saldo da Conta\n")

        // Linhas
        transacoes.forEach { t ->
            val tipoLegivel = if (t.tipo == TipoTransacao.RECEITA) "Receita" else "Despesa"
            csv.append(t.id).append(',')
                .append(escapeCsv(t.descricao)).append(',')
                .append("%.2f".format(t.valor ?: 0.0)).append(',')
                .append(tipoLegivel).append(',')
                .append(requireNotNull(t.data).format(fmtDateTime)).append(',')
                .append(t.categoria?.let { escapeCsv(it.nome) } ?: "").append(',')
                .append(t.conta?.let { escapeCsv(it.nome) } ?: "").append(',')
                .append(t.conta?.saldo?.let { "%.2f".format(it) } ?: "")
                .append('\n')
        }

        return csv.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        return if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            "\"${value.replace("\"", "\"\"")}\""
        else value
    }
}
