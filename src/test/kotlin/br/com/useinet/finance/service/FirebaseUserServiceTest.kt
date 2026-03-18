package br.com.useinet.finance.service

import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FirebaseUserServiceTest {

    @Mock lateinit var usuarioRepository: UsuarioRepository
    @InjectMocks lateinit var firebaseUserService: FirebaseUserService

    private fun tokenMock(
        uid: String = "uid-123",
        email: String = "user@example.com",
        name: String? = "User Name"
    ): FirebaseToken = mock(FirebaseToken::class.java).also {
        whenever(it.uid).thenReturn(uid)
        whenever(it.email).thenReturn(email)
        whenever(it.name).thenReturn(name)
    }

    @Test
    fun findOrCreate_shouldReturnExistingUserByProviderAndUid() {
        val existing = Usuario().apply {
            id = 1L; nome = "Existing"; email = "user@example.com"
            provider = "firebase"; providerId = "uid-123"
        }
        whenever(usuarioRepository.findByProviderAndProviderId("firebase", "uid-123"))
            .thenReturn(Optional.of(existing))

        val result = firebaseUserService.findOrCreate(tokenMock())

        assertThat(result).isEqualTo(existing)
    }

    @Test
    fun findOrCreate_shouldLinkExistingEmailUserToFirebase() {
        val existing = Usuario().apply { id = 2L; nome = "Email User"; email = "user@example.com" }
        whenever(usuarioRepository.findByProviderAndProviderId("firebase", "uid-123"))
            .thenReturn(Optional.empty())
        whenever(usuarioRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.of(existing))
        whenever(usuarioRepository.save(existing)).thenReturn(existing)

        val result = firebaseUserService.findOrCreate(tokenMock())

        assertThat(result.provider).isEqualTo("firebase")
        assertThat(result.providerId).isEqualTo("uid-123")
        verify(usuarioRepository).save(existing)
    }

    @Test
    fun findOrCreate_shouldCreateNewUserWhenNotFound() {
        whenever(usuarioRepository.findByProviderAndProviderId("firebase", "uid-123"))
            .thenReturn(Optional.empty())
        whenever(usuarioRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.empty())
        whenever(usuarioRepository.save(any())).thenAnswer { it.getArgument<Usuario>(0).apply { id = 3L } }

        val result = firebaseUserService.findOrCreate(tokenMock())

        assertThat(result.email).isEqualTo("user@example.com")
        assertThat(result.nome).isEqualTo("User Name")
        assertThat(result.provider).isEqualTo("firebase")
        assertThat(result.providerId).isEqualTo("uid-123")
        verify(usuarioRepository).save(any())
    }

    @Test
    fun findOrCreate_shouldUseEmailAsNomeWhenTokenNameIsNull() {
        whenever(usuarioRepository.findByProviderAndProviderId("firebase", "uid-123"))
            .thenReturn(Optional.empty())
        whenever(usuarioRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.empty())
        whenever(usuarioRepository.save(any())).thenAnswer { it.getArgument<Usuario>(0).apply { id = 4L } }

        val result = firebaseUserService.findOrCreate(tokenMock(name = null))

        assertThat(result.nome).isEqualTo("user@example.com")
    }
}
