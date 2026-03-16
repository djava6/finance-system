package br.com.useinet.finance.dto;

import br.com.useinet.finance.model.Usuario;

public class UserProfileResponse {
    private Long id;
    private String nome;
    private String email;

    public static UserProfileResponse from(Usuario u) {
        UserProfileResponse r = new UserProfileResponse();
        r.id = u.getId();
        r.nome = u.getNome();
        r.email = u.getEmail();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
}
