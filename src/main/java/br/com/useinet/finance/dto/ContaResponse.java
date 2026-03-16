package br.com.useinet.finance.dto;

import br.com.useinet.finance.model.Conta;

public class ContaResponse {
    private Long id;
    private String nome;
    private Double saldo;

    public static ContaResponse from(Conta c) {
        ContaResponse r = new ContaResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.saldo = c.getSaldo();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Double getSaldo() { return saldo; }
}
