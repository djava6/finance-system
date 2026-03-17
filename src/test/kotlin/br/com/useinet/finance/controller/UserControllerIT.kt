package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.UserProfileResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIT {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @MockBean lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
    }

    @BeforeEach
    fun setUp() {
        cleanupUser("profileuser@it.com")
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-profile")
        `when`(mockToken.email).thenReturn("profileuser@it.com")
        `when`(mockToken.name).thenReturn("Profile User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    @Test
    fun getMe_shouldReturn200WithUserData() {
        val response = restTemplate.exchange("/users/me", HttpMethod.GET, HttpEntity<Any>(authHeaders()), UserProfileResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.email).isEqualTo("profileuser@it.com")
        assertThat(response.body!!.nome).isEqualTo("Profile User")
    }

    @Test
    fun getMe_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/users/me", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun updateMe_shouldReturn200WithUpdatedName() {
        val response = restTemplate.exchange(
            "/users/me", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to "Novo Nome"), authHeaders()),
            UserProfileResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.nome).isEqualTo("Novo Nome")
    }

    @Test
    fun updateMe_shouldReturn400WhenNomeIsBlank() {
        val response = restTemplate.exchange(
            "/users/me", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to ""), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
