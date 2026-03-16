package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.LoginRequest;
import br.com.useinet.finance.dto.RegisterRequest;
import br.com.useinet.finance.model.RefreshToken;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");
    }

    private RefreshToken mockRefreshToken() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token-uuid");
        return rt;
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Carlos");
        request.setEmail("carlos@email.com");
        request.setSenha("senha1234");

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getSenha())).thenReturn("encoded");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token123");
        when(refreshTokenService.create(any())).thenReturn(mockRefreshToken());

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getEmail()).isEqualTo("carlos@email.com");
        assertThat(response.getNome()).isEqualTo("Carlos");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Carlos");
        request.setEmail("carlos@email.com");
        request.setSenha("senha1234");

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("carlos@email.com");
        request.setSenha("senha1234");

        Usuario usuario = new Usuario();
        usuario.setNome("Carlos");
        usuario.setEmail("carlos@email.com");

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("token123");
        when(refreshTokenService.create(any())).thenReturn(mockRefreshToken());

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token123");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_shouldThrowWhenNomeIsBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("");
        request.setEmail("valid@email.com");
        request.setSenha("senha1234");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome é obrigatório");
    }

    @Test
    void register_shouldThrowWhenEmailIsInvalid() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Carlos");
        request.setEmail("invalidemail");
        request.setSenha("senha1234");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail inválido");
    }

    @Test
    void loginWithGoogle_shouldThrowForInvalidToken() {
        assertThatThrownBy(() -> authService.loginWithGoogle("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Falha ao verificar token do Google");
    }

    @Test
    void loginWithGoogle_shouldThrowForEmptyToken() {
        assertThatThrownBy(() -> authService.loginWithGoogle(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_shouldThrowWhenSenhaIsTooShort() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Carlos");
        request.setEmail("carlos@email.com");
        request.setSenha("123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha deve ter no mínimo 8 caracteres");
    }
}