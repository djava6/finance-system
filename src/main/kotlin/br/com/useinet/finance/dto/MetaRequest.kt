package br.com.useinet.finance.dto

import java.time.LocalDate

data class MetaRequest(
    val nome: String? = null,
    val valorAlvo: Double? = null,
    val prazo: LocalDate? = null
)
