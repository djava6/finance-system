package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Meta
import java.time.LocalDate

data class MetaResponse(
    val id: Long?,
    val nome: String?,
    val valorAlvo: Double,
    val valorAtual: Double,
    val prazo: LocalDate?,
    val concluida: Boolean,
    val percentual: Double
) {
    companion object {
        fun from(m: Meta): MetaResponse {
            val pct = if (m.valorAlvo > 0) (m.valorAtual / m.valorAlvo) * 100.0 else 0.0
            return MetaResponse(
                id = m.id,
                nome = m.nome,
                valorAlvo = m.valorAlvo,
                valorAtual = m.valorAtual,
                prazo = m.prazo,
                concluida = m.concluida,
                percentual = pct
            )
        }
    }
}
