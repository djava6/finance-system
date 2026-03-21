package br.com.useinet.finance.dto

import jakarta.validation.constraints.NotBlank

data class FcmTokenRequest(
    @field:NotBlank(message = "FCM token é obrigatório.")
    val token: String? = null
)
