package br.com.useinet.finance.dto;

import java.util.List;

public class DashboardResponse {
    private Double saldo;
    private Double totalReceitas;
    private Double totalDespesas;
    private List<TransacaoResponse> ultimasTransacoes;
    private List<DespesaPorCategoriaResponse> despesasPorCategoria;
    private List<EvolucaoMensalResponse> evolucaoMensal;

    public DashboardResponse(Double totalReceitas,
                             Double totalDespesas,
                             List<TransacaoResponse> ultimasTransacoes,
                             List<DespesaPorCategoriaResponse> despesasPorCategoria,
                             List<EvolucaoMensalResponse> evolucaoMensal) {
        this.totalReceitas = totalReceitas;
        this.totalDespesas = totalDespesas;
        this.saldo = totalReceitas - totalDespesas;
        this.ultimasTransacoes = ultimasTransacoes;
        this.despesasPorCategoria = despesasPorCategoria;
        this.evolucaoMensal = evolucaoMensal;
    }

    public Double getSaldo() { return saldo; }
    public Double getTotalReceitas() { return totalReceitas; }
    public Double getTotalDespesas() { return totalDespesas; }
    public List<TransacaoResponse> getUltimasTransacoes() { return ultimasTransacoes; }
    public List<DespesaPorCategoriaResponse> getDespesasPorCategoria() { return despesasPorCategoria; }
    public List<EvolucaoMensalResponse> getEvolucaoMensal() { return evolucaoMensal; }
}
