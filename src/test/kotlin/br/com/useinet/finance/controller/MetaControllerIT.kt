package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.MetaResponse
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

class MetaControllerIT : IntegrationTestBase() {

    @Autowired lateinit var restTemplate: TestRestTemplate

    companion object {
        const val MOCK_TOKEN = "mock-meta-token"
        const val USER_EMAIL = "metauser@it.com"
    }

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update(
            "DELETE FROM metas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", USER_EMAIL
        )
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-meta")
        `when`(mockToken.email).thenReturn(USER_EMAIL)
        `when`(mockToken.name).thenReturn("Meta User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    private fun metaBody(nome: String = "Viagem", valorAlvo: Double = 5000.0) =
        mapOf("nome" to nome, "valorAlvo" to valorAlvo)

    @Test
    fun listar_shouldReturn200() {
        val response = restTemplate.exchange(
            "/metas", HttpMethod.GET, HttpEntity<Any>(authHeaders()), Array<MetaResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
    }

    @Test
    fun criar_shouldReturn201WithNewMeta() {
        val response = restTemplate.postForEntity(
            "/metas",
            HttpEntity(metaBody("Viagem IT"), authHeaders()),
            MetaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.nome).isEqualTo("Viagem IT")
        assertThat(response.body!!.valorAlvo).isEqualTo(5000.0)
        assertThat(response.body!!.valorAtual).isEqualTo(0.0)
        assertThat(response.body!!.concluida).isFalse()
    }

    @Test
    fun criar_shouldReturn400WhenNomeIsBlank() {
        val response = restTemplate.postForEntity(
            "/metas",
            HttpEntity(mapOf("nome" to "", "valorAlvo" to 1000.0), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun criar_shouldReturn400WhenValorAlvoIsZero() {
        val response = restTemplate.postForEntity(
            "/metas",
            HttpEntity(mapOf("nome" to "Carro IT", "valorAlvo" to 0.0), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun depositar_shouldIncreaseValorAtual() {
        val created = restTemplate.postForEntity(
            "/metas",
            HttpEntity(metaBody("Reserva IT"), authHeaders()),
            MetaResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/metas/$id/deposito", HttpMethod.PATCH,
            HttpEntity(mapOf("valor" to 1000.0), authHeaders()),
            MetaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.valorAtual).isEqualTo(1000.0)
        assertThat(response.body!!.percentual).isEqualTo(20.0)
    }

    @Test
    fun depositar_shouldMarkAsConcluidaWhenValorAtingido() {
        val created = restTemplate.postForEntity(
            "/metas",
            HttpEntity(mapOf("nome" to "Meta Pequena IT", "valorAlvo" to 100.0), authHeaders()),
            MetaResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/metas/$id/deposito", HttpMethod.PATCH,
            HttpEntity(mapOf("valor" to 100.0), authHeaders()),
            MetaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.concluida).isTrue()
        assertThat(response.body!!.percentual).isEqualTo(100.0)
    }

    @Test
    fun atualizar_shouldReturn200WithNewValues() {
        val created = restTemplate.postForEntity(
            "/metas",
            HttpEntity(metaBody("Original IT"), authHeaders()),
            MetaResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/metas/$id", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to "Atualizada IT", "valorAlvo" to 8000.0), authHeaders()),
            MetaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.nome).isEqualTo("Atualizada IT")
        assertThat(response.body!!.valorAlvo).isEqualTo(8000.0)
    }

    @Test
    fun deletar_shouldReturn204() {
        val created = restTemplate.postForEntity(
            "/metas",
            HttpEntity(metaBody("Para Deletar IT"), authHeaders()),
            MetaResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/metas/$id", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), Void::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/metas", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
