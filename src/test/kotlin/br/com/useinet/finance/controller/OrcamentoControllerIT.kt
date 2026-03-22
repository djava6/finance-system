package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.OrcamentoResponse
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

class OrcamentoControllerIT : IntegrationTestBase() {

    @Autowired lateinit var restTemplate: TestRestTemplate

    companion object {
        const val MOCK_TOKEN = "mock-orcamento-token"
        const val USER_EMAIL = "orcamentouser@it.com"
    }

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update(
            "DELETE FROM orcamentos WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", USER_EMAIL
        )
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-orcamento")
        `when`(mockToken.email).thenReturn(USER_EMAIL)
        `when`(mockToken.name).thenReturn("Orcamento User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    private fun orcamentoBody(valorLimite: Double = 1000.0, mes: Int = 3, ano: Int = 2026) =
        mapOf("valorLimite" to valorLimite, "mes" to mes, "ano" to ano)

    @Test
    fun listar_shouldReturn200() {
        val response = restTemplate.exchange(
            "/orcamentos", HttpMethod.GET, HttpEntity<Any>(authHeaders()), Array<OrcamentoResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
    }

    @Test
    fun listarPorMes_shouldReturn200() {
        val response = restTemplate.exchange(
            "/orcamentos/mes?mes=3&ano=2026", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Array<OrcamentoResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun criar_shouldReturn201WithNewOrcamento() {
        val response = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(orcamentoBody(), authHeaders()),
            OrcamentoResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.valorLimite).isEqualTo(1000.0)
        assertThat(response.body!!.mes).isEqualTo(3)
        assertThat(response.body!!.ano).isEqualTo(2026)
        assertThat(response.body!!.gasto).isEqualTo(0.0)
    }

    @Test
    fun criar_shouldReturn400WhenValorLimiteIsNull() {
        val response = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(mapOf("mes" to 3, "ano" to 2026), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun criar_shouldReturn400WhenMesIsNull() {
        val response = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(mapOf("valorLimite" to 1000.0, "ano" to 2026), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun criar_shouldReturn400WhenMesIsInvalid() {
        val response = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(mapOf("valorLimite" to 1000.0, "mes" to 13, "ano" to 2026), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun atualizar_shouldReturn200WithNewLimit() {
        val created = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(orcamentoBody(500.0, mes = 4), authHeaders()),
            OrcamentoResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/orcamentos/$id", HttpMethod.PUT,
            HttpEntity(mapOf("valorLimite" to 1500.0, "mes" to 4, "ano" to 2026), authHeaders()),
            OrcamentoResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.valorLimite).isEqualTo(1500.0)
    }

    @Test
    fun deletar_shouldReturn204() {
        val created = restTemplate.postForEntity(
            "/orcamentos",
            HttpEntity(orcamentoBody(mes = 5), authHeaders()),
            OrcamentoResponse::class.java
        )
        val id = created.body!!.id

        val response = restTemplate.exchange(
            "/orcamentos/$id", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), Void::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/orcamentos", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
