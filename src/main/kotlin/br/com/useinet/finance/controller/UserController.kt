package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.UpdateProfileRequest
import br.com.useinet.finance.dto.UserProfileResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.UsuarioRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Perfil do usuário autenticado")
@SecurityRequirement(name = "Bearer Authentication")
class UserController(private val usuarioRepository: UsuarioRepository) {

    companion object {
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário logado")
    fun me(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<UserProfileResponse> =
        ResponseEntity.ok(UserProfileResponse.from(usuario))

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil", description = "Atualiza o nome do usuário logado")
    fun atualizar(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome não pode ser vazio.")
        usuario.nome = request.nome.trim()
        if (!request.email.isNullOrBlank()) {
            val email = request.email.trim()
            if (!EMAIL_PATTERN.matches(email)) throw IllegalArgumentException("E-mail inválido.")
            if (usuarioRepository.findByEmail(email).filter { it.id != usuario.id }.isPresent) {
                throw IllegalArgumentException("E-mail já está em uso.")
            }
            usuario.email = email
        }
        return ResponseEntity.ok(UserProfileResponse.from(usuarioRepository.save(usuario)))
    }
}
