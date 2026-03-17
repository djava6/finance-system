package br.com.useinet.finance.dto

data class DashboardResponse(
    val totalReceitas: Double,
    val totalDespesas: Double,
    val contas: List<ContaResponse>,
    val ultimasTransacoes: List<TransacaoResponse>,
    val despesasPorCategoria: List<DespesaPorCategoriaResponse>,
    val evolucaoMensal: List<EvolucaoMensalResponse>
) {
    val saldo: Double = totalReceitas - totalDespesas
}
