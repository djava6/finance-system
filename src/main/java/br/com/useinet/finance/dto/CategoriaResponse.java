package br.com.useinet.finance.dto;

import br.com.useinet.finance.model.Categoria;

public class CategoriaResponse {
    private Long id;
    private String nome;

    public static CategoriaResponse from(Categoria c) {
        CategoriaResponse r = new CategoriaResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
