package br.com.useinet.finance.dto

data class ImportResultResponse(
    val importadas: Int,
    val duplicatas: Int,
    val erros: Int,
    val mensagensErro: List<String>
)
