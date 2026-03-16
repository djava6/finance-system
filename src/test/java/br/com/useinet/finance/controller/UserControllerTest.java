package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.UpdateProfileRequest;
import br.com.useinet.finance.dto.UserProfileResponse;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserController userController;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Carlos");
        usuario.setEmail("carlos@example.com");
        usuario.setSenha("encodedPassword");
    }

    // --- GET /users/me ---

    @Test
    void me_shouldReturnAuthenticatedUserProfile() {
        ResponseEntity<UserProfileResponse> response = userController.me(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getNome()).isEqualTo("Carlos");
        assertThat(response.getBody().getEmail()).isEqualTo("carlos@example.com");
    }

    // --- PUT /users/me — nome ---

    @Test
    void atualizar_shouldUpdateNome() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos Novo");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        ResponseEntity<UserProfileResponse> response = userController.atualizar(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNome()).isEqualTo("Carlos Novo");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void atualizar_shouldTrimNome() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("  Carlos  ");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        assertThat(usuario.getNome()).isEqualTo("Carlos");
    }

    @Test
    void atualizar_shouldThrowWhenNomeIsNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome(null);

        assertThatThrownBy(() -> userController.atualizar(usuario, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome não pode ser vazio.");
    }

    @Test
    void atualizar_shouldThrowWhenNomeIsBlank() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("   ");

        assertThatThrownBy(() -> userController.atualizar(usuario, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome não pode ser vazio.");
    }

    // --- PUT /users/me — email ---

    @Test
    void atualizar_shouldUpdateEmailWhenValidAndUnique() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("novo@example.com");
        when(usuarioRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        ResponseEntity<UserProfileResponse> response = userController.atualizar(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
    }

    @Test
    void atualizar_shouldTrimEmail() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("  novo@example.com  ");
        when(usuarioRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
    }

    @Test
    void atualizar_shouldThrowWhenEmailIsInvalid() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("notanemail");

        assertThatThrownBy(() -> userController.atualizar(usuario, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail inválido.");
    }

    @Test
    void atualizar_shouldThrowWhenEmailIsTakenByAnotherUser() {
        Usuario outro = new Usuario();
        outro.setId(2L);
        outro.setEmail("outro@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("outro@example.com");
        when(usuarioRepository.findByEmail("outro@example.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> userController.atualizar(usuario, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail já está em uso.");
    }

    @Test
    void atualizar_shouldAllowSameEmailForSameUser() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("carlos@example.com");
        when(usuarioRepository.findByEmail("carlos@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        ResponseEntity<UserProfileResponse> response = userController.atualizar(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void atualizar_shouldSkipEmailUpdateWhenEmailIsNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail(null);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        assertThat(usuario.getEmail()).isEqualTo("carlos@example.com");
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void atualizar_shouldSkipEmailUpdateWhenEmailIsBlank() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setEmail("   ");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        assertThat(usuario.getEmail()).isEqualTo("carlos@example.com");
        verify(usuarioRepository, never()).findByEmail(any());
    }

    // --- PUT /users/me — senha ---

    @Test
    void atualizar_shouldUpdateSenhaWhenValid() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setSenha("novaSenha123");
        when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNovaSenha");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        assertThat(usuario.getSenha()).isEqualTo("encodedNovaSenha");
        verify(passwordEncoder).encode("novaSenha123");
    }

    @Test
    void atualizar_shouldThrowWhenSenhaIsTooShort() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setSenha("123");

        assertThatThrownBy(() -> userController.atualizar(usuario, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha deve ter no mínimo 6 caracteres.");
    }

    @Test
    void atualizar_shouldSkipSenhaUpdateWhenSenhaIsNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNome("Carlos");
        request.setSenha(null);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        userController.atualizar(usuario, request);

        verify(passwordEncoder, never()).encode(any());
        assertThat(usuario.getSenha()).isEqualTo("encodedPassword");
    }
}