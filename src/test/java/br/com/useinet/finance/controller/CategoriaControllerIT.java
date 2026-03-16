package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.CategoriaResponse;
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
class CategoriaControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        var register = Map.of("nome", "Cat User", "email", "catuser@it.com", "senha", "senha123");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() != null) {
            token = auth.getBody().getToken();
        } else {
            var login = Map.of("email", "catuser@it.com", "senha", "senha123");
            token = restTemplate.postForEntity("/auth/login", login, AuthResponse.class).getBody().getToken();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void listar_shouldReturn200WithEmptyOrPopulatedList() {
        ResponseEntity<CategoriaResponse[]> response = restTemplate.exchange(
                "/categories", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                CategoriaResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void criar_shouldReturn201WithNewCategory() {
        var body = Map.of("nome", "Alimentação IT");

        ResponseEntity<CategoriaResponse> response = restTemplate.postForEntity(
                "/categories",
                new HttpEntity<>(body, authHeaders()),
                CategoriaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNome()).isEqualTo("Alimentação IT");
    }

    @Test
    void criar_shouldReturn400WhenNomeIsBlank() {
        var body = Map.of("nome", "");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/categories",
                new HttpEntity<>(body, authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criar_shouldReturn400WhenCategoriaJaExiste() {
        var body = Map.of("nome", "Duplicada IT");
        restTemplate.postForEntity("/categories", new HttpEntity<>(body, authHeaders()), CategoriaResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/categories",
                new HttpEntity<>(body, authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deletar_shouldReturn204WhenCategoryExists() {
        var body = Map.of("nome", "Para Deletar IT");
        ResponseEntity<CategoriaResponse> created = restTemplate.postForEntity(
                "/categories", new HttpEntity<>(body, authHeaders()), CategoriaResponse.class);

        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/categories/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void listar_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/categories", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void renomear_shouldReturn200WithNewName() {
        var createBody = Map.of("nome", "Original IT");
        ResponseEntity<CategoriaResponse> created = restTemplate.postForEntity(
                "/categories", new HttpEntity<>(createBody, authHeaders()), CategoriaResponse.class);
        Long id = created.getBody().getId();

        var updateBody = Map.of("nome", "Renomeada IT");
        ResponseEntity<CategoriaResponse> response = restTemplate.exchange(
                "/categories/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, authHeaders()), CategoriaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNome()).isEqualTo("Renomeada IT");
    }

    @Test
    void renomear_shouldReturn400WhenNomeIsBlank() {
        var createBody = Map.of("nome", "Para Renomear Blank IT");
        ResponseEntity<CategoriaResponse> created = restTemplate.postForEntity(
                "/categories", new HttpEntity<>(createBody, authHeaders()), CategoriaResponse.class);
        Long id = created.getBody().getId();

        var updateBody = Map.of("nome", "");
        ResponseEntity<String> response = restTemplate.exchange(
                "/categories/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
