package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Orcamento

data class OrcamentoResponse(
    val id: Long?,
    val categoriaId: Long?,
    val categoria: String?,
    val valorLimite: Double,
    val mes: Int,
    val ano: Int,
    val gasto: Double,
    val percentual: Double
) {
    companion object {
        fun from(o: Orcamento, gasto: Double): OrcamentoResponse {
            val pct = if (o.valorLimite > 0) (gasto / o.valorLimite) * 100.0 else 0.0
            return OrcamentoResponse(
                id = o.id,
                categoriaId = o.categoria?.id,
                categoria = o.categoria?.nome,
                valorLimite = o.valorLimite,
                mes = o.mes,
                ano = o.ano,
                gasto = gasto,
                percentual = pct
            )
        }
    }
}
