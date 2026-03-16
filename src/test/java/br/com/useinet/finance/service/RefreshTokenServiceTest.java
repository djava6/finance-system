package br.com.useinet.finance.service;

import br.com.useinet.finance.model.RefreshToken;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Usuario usuarioMock() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        return u;
    }

    @Test
    void create_shouldDeleteOldTokenAndCreateNew() {
        Usuario usuario = usuarioMock();

        RefreshToken saved = new RefreshToken();
        saved.setToken("uuid-token");
        saved.setUsuario(usuario);
        saved.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.save(any())).thenReturn(saved);

        RefreshToken result = refreshTokenService.create(usuario);

        verify(refreshTokenRepository).deleteByUsuario(usuario);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result.getToken()).isEqualTo("uuid-token");
    }

    @Test
    void validate_shouldReturnTokenWhenValid() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("valid-token");
        rt.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(rt));

        RefreshToken result = refreshTokenService.validate("valid-token");

        assertThat(result.getToken()).isEqualTo("valid-token");
    }

    @Test
    void validate_shouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void validate_shouldThrowAndDeleteWhenTokenExpired() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("expired-token");
        rt.setExpiryDate(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> refreshTokenService.validate("expired-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");

        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    void deleteByUsuario_shouldDelegateToRepository() {
        Usuario usuario = usuarioMock();

        refreshTokenService.deleteByUsuario(usuario);

        verify(refreshTokenRepository).deleteByUsuario(usuario);
    }
}
