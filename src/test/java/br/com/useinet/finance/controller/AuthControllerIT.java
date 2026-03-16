package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
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
class AuthControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_shouldCreateUserAndReturn201() {
        var request = Map.of("nome", "Carlos", "email", "carlos@it.com", "senha", "senha1234");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo("carlos@it.com");
    }

    @Test
    void register_shouldReturn400WhenEmailAlreadyExists() {
        var request = Map.of("nome", "Carlos", "email", "duplicado@it.com", "senha", "senha1234");
        restTemplate.postForEntity("/auth/register", request, AuthResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        var register = Map.of("nome", "Login Test", "email", "logintest@it.com", "senha", "senha123");
        restTemplate.postForEntity("/auth/register", register, AuthResponse.class);

        var login = Map.of("email", "logintest@it.com", "senha", "senha123");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void login_shouldReturn403ForInvalidCredentials() {
        var login = Map.of("email", "naoexiste@it.com", "senha", "errada");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", login, String.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void refresh_shouldReturn200WithNewToken() {
        var register = Map.of("nome", "Refresh User", "email", "refresh@it.com", "senha", "senha1234");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() == null) {
            var login = Map.of("email", "refresh@it.com", "senha", "senha1234");
            auth = restTemplate.postForEntity("/auth/login", login, AuthResponse.class);
        }
        String refreshToken = auth.getBody().getRefreshToken();

        var request = Map.of("refreshToken", refreshToken);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/auth/refresh", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void refresh_shouldReturn400WhenTokenInvalid() {
        var request = Map.of("refreshToken", "invalid-token-uuid");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/refresh", request, String.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void logout_shouldReturn204() {
        var register = Map.of("nome", "Logout User", "email", "logout@it.com", "senha", "senha1234");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() == null) {
            var login = Map.of("email", "logout@it.com", "senha", "senha1234");
            auth = restTemplate.postForEntity("/auth/login", login, AuthResponse.class);
        }
        String jwtToken = auth.getBody().getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void register_shouldReturn400WhenNomeIsBlank() {
        var request = Map.of("nome", "", "email", "nomeblank@it.com", "senha", "senha1234");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_shouldReturn400WhenEmailIsInvalid() {
        var request = Map.of("nome", "Test", "email", "invalidemail", "senha", "senha1234");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_shouldReturn400WhenSenhaIsTooShort() {
        var request = Map.of("nome", "Test", "email", "shortpwd@it.com", "senha", "123");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void googleAuth_shouldReturn400ForInvalidToken() {
        var request = Map.of("idToken", "invalid-google-token");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/google", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void googleAuth_shouldReturn400ForEmptyToken() {
        var request = Map.of("idToken", "");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/google", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}