package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.TransacaoResponse;
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
class TransacaoControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        var register = Map.of("nome", "Carlos", "email", "carlos.transacao@it.com", "senha", "senha1234");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() != null && auth.getBody().getToken() != null) {
            token = auth.getBody().getToken();
        } else {
            var login = Map.of("email", "carlos.transacao@it.com", "senha", "senha1234");
            ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity("/auth/login", login, AuthResponse.class);
            token = loginResp.getBody().getToken();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void criar_shouldReturn201AndTransacao() {
        var request = Map.of(
                "descricao", "Salário",
                "valor", 5000.0,
                "tipo", "RECEITA"
        );

        ResponseEntity<TransacaoResponse> response = restTemplate.exchange(
                "/transactions",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                TransacaoResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getDescricao()).isEqualTo("Salário");
        assertThat(response.getBody().getValor()).isEqualTo(5000.0);
    }

    @Test
    void listar_shouldReturnTransacoesDoUsuario() {
        var request = Map.of("descricao", "Mercado", "valor", 200.0, "tipo", "DESPESA");
        restTemplate.exchange("/transactions", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), TransacaoResponse.class);

        ResponseEntity<TransacaoResponse[]> response = restTemplate.exchange(
                "/transactions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                TransacaoResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void deletar_shouldReturn204() {
        var request = Map.of("descricao", "A deletar", "valor", 50.0, "tipo", "DESPESA");
        ResponseEntity<TransacaoResponse> created = restTemplate.exchange(
                "/transactions", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), TransacaoResponse.class
        );

        Long id = created.getBody().getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/transactions/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void listar_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/transactions", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void atualizar_shouldReturn200WithUpdatedData() {
        var createReq = Map.of("descricao", "Original", "valor", 100.0, "tipo", "RECEITA");
        ResponseEntity<TransacaoResponse> created = restTemplate.exchange(
                "/transactions", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), TransacaoResponse.class);

        Long id = created.getBody().getId();

        var updateReq = Map.of("descricao", "Atualizado", "valor", 200.0, "tipo", "RECEITA");
        ResponseEntity<TransacaoResponse> response = restTemplate.exchange(
                "/transactions/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateReq, authHeaders()), TransacaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDescricao()).isEqualTo("Atualizado");
        assertThat(response.getBody().getValor()).isEqualTo(200.0);
    }

    @Test
    void listar_comFiltroData_shouldReturn200() {
        var createReq = Map.of("descricao", "Filtrado", "valor", 50.0, "tipo", "DESPESA");
        restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), TransacaoResponse.class);

        ResponseEntity<TransacaoResponse[]> response = restTemplate.exchange(
                "/transactions?inicio=2020-01-01&fim=2099-12-31",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), TransacaoResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void listar_comDataInvalida_shouldReturn400() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/transactions?inicio=2025-06-01&fim=2025-01-01",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void exportarCsv_shouldReturn200WithCsvContentType() {
        var createReq = Map.of("descricao", "CSV Export", "valor", 300.0, "tipo", "RECEITA");
        restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders()), TransacaoResponse.class);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/transactions/export/csv", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
    }
}