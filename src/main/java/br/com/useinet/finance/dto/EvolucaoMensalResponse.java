package br.com.useinet.finance.dto;

public class EvolucaoMensalResponse {
    private int ano;
    private int mes;
    private Double totalReceitas;
    private Double totalDespesas;

    public EvolucaoMensalResponse(int ano, int mes, Double totalReceitas, Double totalDespesas) {
        this.ano = ano;
        this.mes = mes;
        this.totalReceitas = totalReceitas;
        this.totalDespesas = totalDespesas;
    }

    public int getAno() { return ano; }
    public int getMes() { return mes; }
    public Double getTotalReceitas() { return totalReceitas; }
    public Double getTotalDespesas() { return totalDespesas; }
}
