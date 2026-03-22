package br.com.useinet.finance.dto

import jakarta.validation.constraints.NotBlank

data class ReciboUrlRequest(
    @field:NotBlank val reciboUrl: String
)
