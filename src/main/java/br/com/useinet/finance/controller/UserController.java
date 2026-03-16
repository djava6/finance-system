package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.UpdateProfileRequest;
import br.com.useinet.finance.dto.UserProfileResponse;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Perfil do usuário autenticado")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário logado")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(UserProfileResponse.from(usuario));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil", description = "Atualiza o nome do usuário logado")
    public ResponseEntity<UserProfileResponse> atualizar(@AuthenticationPrincipal Usuario usuario,
                                                         @RequestBody UpdateProfileRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser vazio.");
        }
        usuario.setNome(request.getNome().trim());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("E-mail inválido.");
            }
            if (usuarioRepository.findByEmail(email).filter(u -> !u.getId().equals(usuario.getId())).isPresent()) {
                throw new IllegalArgumentException("E-mail já está em uso.");
            }
            usuario.setEmail(email);
        }
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            if (request.getSenha().length() < 6) {
                throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres.");
            }
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        return ResponseEntity.ok(UserProfileResponse.from(usuarioRepository.save(usuario)));
    }
}
