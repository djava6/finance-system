package br.com.useinet.finance.dto;

import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;

import java.time.LocalDateTime;

public class TransacaoResponse {
    private Long id;
    private String descricao;
    private Double valor;
    private TipoTransacao tipo;
    private LocalDateTime data;
    private String categoria;
    private String conta;

    public static TransacaoResponse from(Transacao transacao) {
        TransacaoResponse response = new TransacaoResponse();
        response.id = transacao.getId();
        response.descricao = transacao.getDescricao();
        response.valor = transacao.getValor();
        response.tipo = transacao.getTipo();
        response.data = transacao.getData();
        response.categoria = transacao.getCategoria() != null
                ? transacao.getCategoria().getNome()
                : null;
        response.conta = transacao.getConta() != null
                ? transacao.getConta().getNome()
                : null;
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public LocalDateTime getData() {
        return data;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getConta() {
        return conta;
    }
}