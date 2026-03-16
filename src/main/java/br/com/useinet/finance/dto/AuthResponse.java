package br.com.useinet.finance.dto;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String nome;
    private String email;

    public AuthResponse(String token, String refreshToken, String nome, String email) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.nome = nome;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
}
