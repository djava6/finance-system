package br.com.useinet.finance.dto;

public class ContaRequest {
    private String nome;
    private Double saldo;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
}
