package br.com.useinet.finance.service

import br.com.useinet.finance.dto.MetaDepositoRequest
import br.com.useinet.finance.dto.MetaRequest
import br.com.useinet.finance.dto.MetaResponse
import br.com.useinet.finance.model.Meta
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.MetaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetaService(
    private val metaRepository: MetaRepository,
    private val notificationService: NotificationService
) {

    @Transactional(readOnly = true)
    fun listar(usuario: Usuario): List<MetaResponse> =
        metaRepository.findByUsuario(usuario).map { MetaResponse.from(it) }

    @Transactional
    fun criar(request: MetaRequest, usuario: Usuario): MetaResponse {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da meta é obrigatório.")
        val valorAlvo = request.valorAlvo ?: throw IllegalArgumentException("Valor alvo é obrigatório.")
        if (valorAlvo <= 0) throw IllegalArgumentException("Valor alvo deve ser maior que zero.")

        val meta = Meta().apply {
            this.usuario = usuario
            this.nome = request.nome
            this.valorAlvo = valorAlvo
            this.prazo = request.prazo
        }
        return MetaResponse.from(metaRepository.save(meta))
    }

    @Transactional
    fun atualizar(id: Long, request: MetaRequest, usuario: Usuario): MetaResponse {
        val meta = metaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Meta não encontrada.") }
        if (!request.nome.isNullOrBlank()) meta.nome = request.nome
        if (request.valorAlvo != null) {
            if (request.valorAlvo <= 0) throw IllegalArgumentException("Valor alvo deve ser maior que zero.")
            meta.valorAlvo = request.valorAlvo
        }
        meta.prazo = request.prazo
        return MetaResponse.from(metaRepository.save(meta))
    }

    @Transactional
    fun depositar(id: Long, request: MetaDepositoRequest, usuario: Usuario): MetaResponse {
        val valor = request.valor ?: throw IllegalArgumentException("Valor do depósito é obrigatório.")
        if (valor <= 0) throw IllegalArgumentException("Valor do depósito deve ser maior que zero.")

        val meta = metaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Meta não encontrada.") }
        if (meta.concluida) throw IllegalStateException("Meta já foi concluída.")

        meta.valorAtual += valor
        if (meta.valorAtual >= meta.valorAlvo) {
            meta.valorAtual = meta.valorAlvo
            meta.concluida = true
            notificationService.send(
                usuario,
                "Meta atingida!",
                "Parabéns! Você concluiu a meta \"${meta.nome}\"."
            )
        }
        return MetaResponse.from(metaRepository.save(meta))
    }

    @Transactional
    fun deletar(id: Long, usuario: Usuario) {
        val meta = metaRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Meta não encontrada.") }
        metaRepository.delete(meta)
    }
}
