package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ContaRequest
import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ContaService(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
) {

    @Transactional(readOnly = true)
    fun listar(usuario: Usuario): List<ContaResponse> =
        contaRepository.findByUsuario(usuario).map { ContaResponse.from(it) }

    @Transactional
    fun criar(request: ContaRequest, usuario: Usuario): ContaResponse {
        validate(request)
        val saldoInicial = request.saldo ?: 0.0
        val conta = Conta().apply {
            this.nome = requireNotNull(request.nome).trim()
            this.saldo = 0.0
            this.numeroConta = request.numeroConta?.trim()?.ifBlank { null }
            this.agencia = request.agencia?.trim()?.ifBlank { null }
            this.usuario = usuario
        }
        val savedConta = contaRepository.save(conta)

        if (saldoInicial > 0.0) {
            val patrimonio = categoriaRepository.findByNome("Patrimônio").orElse(null)
            transacaoRepository.save(Transacao().apply {
                this.descricao = "Saldo inicial – ${savedConta.nome}"
                this.valor = saldoInicial
                this.tipo = TipoTransacao.RECEITA
                this.data = LocalDate.now()
                this.conta = savedConta
                this.usuario = usuario
                this.categoria = patrimonio
            })
            savedConta.saldo = saldoInicial
            contaRepository.save(savedConta)
        }

        return ContaResponse.from(savedConta)
    }

    @Transactional
    fun atualizar(id: Long, request: ContaRequest, usuario: Usuario): ContaResponse {
        validate(request)
        val conta = contaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Conta não encontrada.") }
        conta.nome = requireNotNull(request.nome).trim()
        conta.numeroConta = request.numeroConta?.trim()?.ifBlank { null }
        conta.agencia = request.agencia?.trim()?.ifBlank { null }
        return ContaResponse.from(contaRepository.save(conta))
    }

    @Transactional
    fun deletar(id: Long, usuario: Usuario) {
        val conta = contaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Conta não encontrada.") }
        contaRepository.delete(conta)
    }

    private fun validate(request: ContaRequest) {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da conta é obrigatório.")
    }
}
