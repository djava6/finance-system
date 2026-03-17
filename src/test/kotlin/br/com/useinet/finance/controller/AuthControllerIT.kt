package br.com.useinet.finance.controller

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
class AuthControllerIT {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @MockBean lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
    }

    @BeforeEach
    fun setUp() {
        cleanupUser("authtest@it.com")
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("test-uid-auth")
        `when`(mockToken.email).thenReturn("authtest@it.com")
        `when`(mockToken.name).thenReturn("Auth Test User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    @Test
    fun unauthenticated_shouldReturn401() {
        val response = restTemplate.getForEntity("/contas", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun validFirebaseToken_shouldAllowAccess() {
        val headers = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }
        val response = restTemplate.exchange("/contas", HttpMethod.GET, HttpEntity<Any>(headers), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun invalidFirebaseToken_shouldReturn401() {
        val headers = HttpHeaders().apply { setBearerAuth("invalid-token-not-stubbed") }
        val response = restTemplate.exchange("/contas", HttpMethod.GET, HttpEntity<Any>(headers), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
