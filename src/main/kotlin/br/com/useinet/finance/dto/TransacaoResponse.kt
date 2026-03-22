package br.com.useinet.finance.dto

import br.com.useinet.finance.model.FrequenciaRecorrencia
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import java.time.LocalDate

data class TransacaoResponse(
    val id: Long?,
    val descricao: String?,
    val valor: Double?,
    val tipo: TipoTransacao?,
    val data: LocalDate?,
    val categoriaId: Long?,
    val categoria: String?,
    val contaId: Long?,
    val conta: String?,
    val recorrente: Boolean,
    val frequencia: FrequenciaRecorrencia?,
    val proximaOcorrencia: LocalDate?,
    val reciboUrl: String?
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
            conta = t.conta?.nome,
            recorrente = t.recorrente,
            frequencia = t.frequencia,
            proximaOcorrencia = t.proximaOcorrencia,
            reciboUrl = t.reciboUrl
        )
    }
}
