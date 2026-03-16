package br.com.useinet.finance.dto;

public class DespesaPorCategoriaResponse {
    private String categoria;
    private Double total;

    public DespesaPorCategoriaResponse(String categoria, Double total) {
        this.categoria = categoria;
        this.total = total;
    }

    public String getCategoria() {
        return categoria;
    }

    public Double getTotal() {
        return total;
    }
}