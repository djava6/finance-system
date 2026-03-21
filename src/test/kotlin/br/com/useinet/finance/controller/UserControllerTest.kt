package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.UpdateProfileRequest
import br.com.useinet.finance.dto.UserProfileResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.UsuarioRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock lateinit var usuarioRepository: UsuarioRepository
    @InjectMocks lateinit var userController: UserController

    private lateinit var usuario: Usuario

    @BeforeEach
    fun setUp() {
        usuario = Usuario().apply {
            id = 1L
            nome = "Carlos"
            email = "carlos@example.com"
        }
    }

    // me() and updateNome are covered end-to-end by UserControllerIT.
    // This unit test focuses on email validation logic not exercised at the IT level.

    @Test
    fun atualizar_shouldThrowWhenNomeIsNull() {
        assertThatThrownBy { userController.atualizar(usuario, UpdateProfileRequest(nome = null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Nome não pode ser vazio.")
    }

    @Test
    fun atualizar_shouldUpdateEmailWhenValidAndUnique() {
        `when`(usuarioRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty())
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        val response = userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "novo@example.com"))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(usuario.email).isEqualTo("novo@example.com")
    }

    @Test
    fun atualizar_shouldTrimEmail() {
        `when`(usuarioRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty())
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "  novo@example.com  "))
        assertThat(usuario.email).isEqualTo("novo@example.com")
    }

    @Test
    fun atualizar_shouldThrowWhenEmailIsInvalid() {
        assertThatThrownBy { userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "notanemail")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail inválido.")
    }

    @Test
    fun atualizar_shouldThrowWhenEmailIsTakenByAnotherUser() {
        val outro = Usuario().apply { id = 2L; email = "outro@example.com" }
        `when`(usuarioRepository.findByEmail("outro@example.com")).thenReturn(Optional.of(outro))
        assertThatThrownBy { userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "outro@example.com")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail já está em uso.")
    }

    @Test
    fun atualizar_shouldAllowSameEmailForSameUser() {
        `when`(usuarioRepository.findByEmail("carlos@example.com")).thenReturn(Optional.of(usuario))
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        val response = userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "carlos@example.com"))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(usuarioRepository).save(usuario)
    }

    @Test
    fun atualizar_shouldSkipEmailUpdateWhenEmailIsNull() {
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = null))
        assertThat(usuario.email).isEqualTo("carlos@example.com")
        verify(usuarioRepository, never()).findByEmail(anyString())
    }

    @Test
    fun atualizar_shouldSkipEmailUpdateWhenEmailIsBlank() {
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "   "))
        assertThat(usuario.email).isEqualTo("carlos@example.com")
        verify(usuarioRepository, never()).findByEmail(anyString())
    }

    // ─── EMAIL_PATTERN edge cases ──────────────────────────────────────────────

    @Test
    fun atualizar_shouldRejectEmailMissingDomain() {
        // "user@" has no domain part — must fail regex
        assertThatThrownBy { userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "user@")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail inválido.")
    }

    @Test
    fun atualizar_shouldRejectEmailMissingLocalPart() {
        // "@domain.com" has empty local part — must fail regex
        assertThatThrownBy { userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "@domain.com")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("E-mail inválido.")
    }

    @Test
    fun atualizar_shouldAcceptEmailWithPlus() {
        // "user+tag@domain.com" — '+' is in the allowed charset
        `when`(usuarioRepository.findByEmail("user+tag@domain.com")).thenReturn(Optional.empty())
        `when`(usuarioRepository.save(usuario)).thenReturn(usuario)
        val response = userController.atualizar(usuario, UpdateProfileRequest(nome = "Carlos", email = "user+tag@domain.com"))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(usuario.email).isEqualTo("user+tag@domain.com")
    }
}
