package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.ContaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ContaControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        var register = Map.of("nome", "Conta User", "email", "contauser@it.com", "senha", "senha123");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() != null && auth.getBody().getToken() != null) {
            token = auth.getBody().getToken();
        } else {
            var login = Map.of("email", "contauser@it.com", "senha", "senha123");
            token = restTemplate.postForEntity("/auth/login", login, AuthResponse.class).getBody().getToken();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void listar_shouldReturn200() {
        ResponseEntity<ContaResponse[]> response = restTemplate.exchange(
                "/contas", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ContaResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void criar_shouldReturn201() {
        var body = Map.of("nome", "Conta Corrente", "saldo", 1000.0);

        ResponseEntity<ContaResponse> response = restTemplate.postForEntity(
                "/contas",
                new HttpEntity<>(body, authHeaders()),
                ContaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getNome()).isEqualTo("Conta Corrente");
        assertThat(response.getBody().getSaldo()).isEqualTo(1000.0);
    }

    @Test
    void criar_shouldReturn400WhenNomeIsBlank() {
        var body = Map.of("nome", "", "saldo", 0.0);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/contas",
                new HttpEntity<>(body, authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void atualizar_shouldReturn200() {
        var body = Map.of("nome", "Poupança IT", "saldo", 500.0);
        ResponseEntity<ContaResponse> created = restTemplate.postForEntity(
                "/contas", new HttpEntity<>(body, authHeaders()), ContaResponse.class);
        Long id = created.getBody().getId();

        var update = Map.of("nome", "Poupança Atualizada", "saldo", 600.0);
        ResponseEntity<ContaResponse> response = restTemplate.exchange(
                "/contas/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()),
                ContaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNome()).isEqualTo("Poupança Atualizada");
    }

    @Test
    void deletar_shouldReturn204() {
        var body = Map.of("nome", "Para Deletar", "saldo", 0.0);
        ResponseEntity<ContaResponse> created = restTemplate.postForEntity(
                "/contas", new HttpEntity<>(body, authHeaders()), ContaResponse.class);
        Long id = created.getBody().getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/contas/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void listar_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/contas", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
