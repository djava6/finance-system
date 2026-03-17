package br.com.useinet.finance.dto

data class EvolucaoMensalResponse(
    val ano: Int,
    val mes: Int,
    val totalReceitas: Double,
    val totalDespesas: Double
)
