package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ContaRequest
import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.ContaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContaService(private val contaRepository: ContaRepository) {

    fun listar(usuario: Usuario): List<ContaResponse> =
        contaRepository.findByUsuario(usuario).map { ContaResponse.from(it) }

    @Transactional
    fun criar(request: ContaRequest, usuario: Usuario): ContaResponse {
        validate(request)
        val conta = Conta().apply {
            this.nome = request.nome!!.trim()
            this.saldo = request.saldo ?: 0.0
            this.usuario = usuario
        }
        return ContaResponse.from(contaRepository.save(conta))
    }

    @Transactional
    fun atualizar(id: Long, request: ContaRequest, usuario: Usuario): ContaResponse {
        validate(request)
        val conta = contaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Conta não encontrada.") }
        conta.nome = request.nome!!.trim()
        if (request.saldo != null) conta.saldo = request.saldo
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
