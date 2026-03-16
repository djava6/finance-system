package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.UserProfileResponse;
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
class UserControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        var register = Map.of("nome", "Profile User", "email", "profileuser@it.com", "senha", "senha123");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() != null && auth.getBody().getToken() != null) {
            token = auth.getBody().getToken();
        } else {
            var login = Map.of("email", "profileuser@it.com", "senha", "senha123");
            token = restTemplate.postForEntity("/auth/login", login, AuthResponse.class).getBody().getToken();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void getMe_shouldReturn200WithUserData() {
        ResponseEntity<UserProfileResponse> response = restTemplate.exchange(
                "/users/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                UserProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("profileuser@it.com");
        assertThat(response.getBody().getNome()).isEqualTo("Profile User");
    }

    @Test
    void getMe_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/users/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateMe_shouldReturn200WithUpdatedName() {
        var body = Map.of("nome", "Novo Nome");

        ResponseEntity<UserProfileResponse> response = restTemplate.exchange(
                "/users/me", HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()),
                UserProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNome()).isEqualTo("Novo Nome");
    }

    @Test
    void updateMe_shouldReturn400WhenNomeIsBlank() {
        var body = Map.of("nome", "");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/me", HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateMe_shouldReturn400WhenSenhaIsTooShort() {
        var body = Map.of("nome", "Valid Name", "senha", "123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/me", HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
