package br.com.useinet.finance.dto

import jakarta.validation.constraints.NotBlank

data class CategoriaRequest(
    @field:NotBlank(message = "Nome da categoria é obrigatório.")
    val nome: String? = null
)
