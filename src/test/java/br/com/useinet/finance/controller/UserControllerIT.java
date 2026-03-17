package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.UserProfileResponse;
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
class UserControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FirebaseAuth firebaseAuth;

    static final String MOCK_TOKEN = "mock-firebase-token";

    @BeforeEach
    void setUp() throws Exception {
        cleanupUser("profileuser@it.com");

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-profile");
        when(mockToken.getEmail()).thenReturn("profileuser@it.com");
        when(mockToken.getName()).thenReturn("Profile User");
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
