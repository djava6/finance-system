package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.LoginRequest;
import br.com.useinet.finance.dto.RefreshRequest;
import br.com.useinet.finance.dto.RegisterRequest;
import br.com.useinet.finance.model.RefreshToken;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        if (request.getSenha() == null || request.getSenha().length() < 8) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 8 caracteres.");
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuarioRepository.save(usuario);

        return buildAuthResponse(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        return buildAuthResponse(usuario);
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken rt = refreshTokenService.validate(request.getRefreshToken());
        Usuario usuario = rt.getUsuario();
        return buildAuthResponse(usuario);
    }

    public void logout(Usuario usuario) {
        refreshTokenService.deleteByUsuario(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario);
        String refreshToken = refreshTokenService.create(usuario).getToken();
        return new AuthResponse(token, refreshToken, usuario.getNome(), usuario.getEmail());
    }
}
