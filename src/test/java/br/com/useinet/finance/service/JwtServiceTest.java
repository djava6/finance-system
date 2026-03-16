package br.com.useinet.finance.service;

import br.com.useinet.finance.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // 32 bytes = 256 bits, minimum for HMAC-SHA256
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-for-testing-only".getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    private Usuario mockUsuario() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        u.setSenha("encoded");
        return u;
    }

    @Test
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateToken(mockUsuario());
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        Usuario usuario = mockUsuario();
        String token = jwtService.generateToken(usuario);
        assertThat(jwtService.extractUsername(token)).isEqualTo("carlos@email.com");
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        Usuario usuario = mockUsuario();
        String token = jwtService.generateToken(usuario);
        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        Usuario u1 = mockUsuario();
        Usuario u2 = new Usuario();
        u2.setNome("Outro");
        u2.setEmail("outro@email.com");
        u2.setSenha("encoded");

        String token = jwtService.generateToken(u1);
        assertThat(jwtService.isTokenValid(token, u2)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        Usuario usuario = mockUsuario();
        String token = jwtService.generateToken(usuario);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, usuario))
                .isInstanceOf(Exception.class);
    }
}
