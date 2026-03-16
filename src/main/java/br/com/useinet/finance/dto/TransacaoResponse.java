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
    private Long categoriaId;
    private String categoria;
    private Long contaId;
    private String conta;

    public static TransacaoResponse from(Transacao transacao) {
        TransacaoResponse response = new TransacaoResponse();
        response.id = transacao.getId();
        response.descricao = transacao.getDescricao();
        response.valor = transacao.getValor();
        response.tipo = transacao.getTipo();
        response.data = transacao.getData();
        if (transacao.getCategoria() != null) {
            response.categoriaId = transacao.getCategoria().getId();
            response.categoria = transacao.getCategoria().getNome();
        }
        if (transacao.getConta() != null) {
            response.contaId = transacao.getConta().getId();
            response.conta = transacao.getConta().getNome();
        }
        return response;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public Double getValor() { return valor; }
    public TipoTransacao getTipo() { return tipo; }
    public LocalDateTime getData() { return data; }
    public Long getCategoriaId() { return categoriaId; }
    public String getCategoria() { return categoria; }
    public Long getContaId() { return contaId; }
    public String getConta() { return conta; }
}