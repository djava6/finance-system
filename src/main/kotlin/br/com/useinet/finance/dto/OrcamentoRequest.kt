package br.com.useinet.finance.dto

data class OrcamentoRequest(
    val categoriaId: Long? = null,
    val valorLimite: Double? = null,
    val mes: Int? = null,
    val ano: Int? = null
)
