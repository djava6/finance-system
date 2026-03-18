package br.com.useinet.finance.dto

import jakarta.validation.constraints.NotBlank

data class ContaRequest(
    @field:NotBlank(message = "Nome da conta é obrigatório.")
    val nome: String? = null,
    val saldo: Double? = null
)
