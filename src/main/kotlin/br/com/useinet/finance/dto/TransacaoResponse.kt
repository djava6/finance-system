package br.com.useinet.finance.dto

import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import java.time.LocalDateTime

data class TransacaoResponse(
    val id: Long?,
    val descricao: String?,
    val valor: Double?,
    val tipo: TipoTransacao?,
    val data: LocalDateTime?,
    val categoriaId: Long?,
    val categoria: String?,
    val contaId: Long?,
    val conta: String?
) {
    companion object {
        fun from(t: Transacao) = TransacaoResponse(
            id = t.id,
            descricao = t.descricao,
            valor = t.valor,
            tipo = t.tipo,
            data = t.data,
            categoriaId = t.categoria?.id,
            categoria = t.categoria?.nome,
            contaId = t.conta?.id,
            conta = t.conta?.nome
        )
    }
}
