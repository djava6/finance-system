package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.support.IntegrationTestBase
import com.google.firebase.auth.FirebaseToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

class ContaControllerIT : IntegrationTestBase() {

    @Autowired lateinit var restTemplate: TestRestTemplate

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
    }

    @BeforeEach
    fun setUp() {
        cleanupUser("contauser@it.com")
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-conta")
        `when`(mockToken.email).thenReturn("contauser@it.com")
        `when`(mockToken.name).thenReturn("Conta User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    @Test
    fun listar_shouldReturn200() {
        val response = restTemplate.exchange("/contas", HttpMethod.GET, HttpEntity<Any>(authHeaders()), Array<ContaResponse>::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
    }

    @Test
    fun criar_shouldReturn201() {
        val response = restTemplate.postForEntity(
            "/contas",
            HttpEntity(mapOf("nome" to "Conta Corrente", "saldo" to 1000.0), authHeaders()),
            ContaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.nome).isEqualTo("Conta Corrente")
        assertThat(response.body!!.saldo).isEqualTo(1000.0)
    }

    @Test
    fun criar_shouldReturn400WhenNomeIsBlank() {
        val response = restTemplate.postForEntity(
            "/contas",
            HttpEntity(mapOf("nome" to "", "saldo" to 0.0), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun atualizar_shouldReturn200() {
        val created = restTemplate.postForEntity(
            "/contas",
            HttpEntity(mapOf("nome" to "Poupança IT", "saldo" to 500.0), authHeaders()),
            ContaResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange(
            "/contas/$id", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to "Poupança Atualizada", "saldo" to 600.0), authHeaders()),
            ContaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.nome).isEqualTo("Poupança Atualizada")
    }

    @Test
    fun deletar_shouldReturn204() {
        val created = restTemplate.postForEntity(
            "/contas",
            HttpEntity(mapOf("nome" to "Para Deletar", "saldo" to 0.0), authHeaders()),
            ContaResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange("/contas/$id", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), Void::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/contas", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
