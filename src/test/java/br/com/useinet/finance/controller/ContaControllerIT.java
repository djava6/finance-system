package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.ContaResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContaControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FirebaseAuth firebaseAuth;

    static final String MOCK_TOKEN = "mock-firebase-token";

    @BeforeEach
    void setUp() throws Exception {
        cleanupUser("contauser@it.com");

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-conta");
        when(mockToken.getEmail()).thenReturn("contauser@it.com");
        when(mockToken.getName()).thenReturn("Conta User");
        when(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken);
    }

    private void cleanupUser(String email) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email);
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email);
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(MOCK_TOKEN);
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
