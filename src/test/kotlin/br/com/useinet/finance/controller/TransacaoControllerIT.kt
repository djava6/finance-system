package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.PageResponse
import br.com.useinet.finance.dto.TransacaoResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.nio.charset.StandardCharsets

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransacaoControllerIT {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var objectMapper: ObjectMapper
    @MockBean lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
    }

    @BeforeEach
    fun setUp() {
        cleanupUser("carlos.transacao@it.com")
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-transacao")
        `when`(mockToken.email).thenReturn("carlos.transacao@it.com")
        `when`(mockToken.name).thenReturn("Carlos")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    @Test
    fun criar_shouldReturn201AndTransacao() {
        val response = restTemplate.exchange(
            "/transactions", HttpMethod.POST,
            HttpEntity(mapOf("descricao" to "Salário", "valor" to 5000.0, "tipo" to "RECEITA"), authHeaders()),
            TransacaoResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.descricao).isEqualTo("Salário")
        assertThat(response.body!!.valor).isEqualTo(5000.0)
    }

    @Test
    fun listar_shouldReturnTransacoesDoUsuario() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "Mercado", "valor" to 200.0, "tipo" to "DESPESA"), authHeaders()), TransacaoResponse::class.java)
        val response = restTemplate.exchange("/transactions", HttpMethod.GET, HttpEntity<Any>(authHeaders()), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val page: PageResponse<TransacaoResponse> = objectMapper.readValue(response.body!!)
        assertThat(page.content).isNotEmpty
    }

    @Test
    fun deletar_shouldReturn204() {
        val created = restTemplate.exchange(
            "/transactions", HttpMethod.POST,
            HttpEntity(mapOf("descricao" to "A deletar", "valor" to 50.0, "tipo" to "DESPESA"), authHeaders()),
            TransacaoResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange("/transactions/$id", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), Void::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/transactions", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun atualizar_shouldReturn200WithUpdatedData() {
        val created = restTemplate.exchange(
            "/transactions", HttpMethod.POST,
            HttpEntity(mapOf("descricao" to "Original", "valor" to 100.0, "tipo" to "RECEITA"), authHeaders()),
            TransacaoResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange(
            "/transactions/$id", HttpMethod.PUT,
            HttpEntity(mapOf("descricao" to "Atualizado", "valor" to 200.0, "tipo" to "RECEITA"), authHeaders()),
            TransacaoResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.descricao).isEqualTo("Atualizado")
        assertThat(response.body!!.valor).isEqualTo(200.0)
    }

    @Test
    fun listar_comFiltroData_shouldReturn200() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "Filtrado", "valor" to 50.0, "tipo" to "DESPESA"), authHeaders()), TransacaoResponse::class.java)
        val response = restTemplate.exchange("/transactions?inicio=2020-01-01&fim=2099-12-31", HttpMethod.GET, HttpEntity<Any>(authHeaders()), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val page: PageResponse<TransacaoResponse> = objectMapper.readValue(response.body!!)
        assertThat(page.totalElements).isGreaterThanOrEqualTo(1)
    }

    @Test
    fun listar_comDataInvalida_shouldReturn400() {
        val response = restTemplate.exchange("/transactions?inicio=2025-06-01&fim=2025-01-01", HttpMethod.GET, HttpEntity<Any>(authHeaders()), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun exportarCsv_shouldReturn200WithCsvContentType() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "CSV Export", "valor" to 300.0, "tipo" to "RECEITA"), authHeaders()), TransacaoResponse::class.java)
        val response = restTemplate.exchange("/transactions/export/csv", HttpMethod.GET, HttpEntity<Any>(authHeaders()), ByteArray::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.headers.contentType.toString()).contains("text/csv")
    }

    @Test
    fun exportarCsv_shouldContainContaColumn() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "CSV Conta", "valor" to 100.0, "tipo" to "DESPESA"), authHeaders()), TransacaoResponse::class.java)
        val response = restTemplate.exchange("/transactions/export/csv", HttpMethod.GET, HttpEntity<Any>(authHeaders()), ByteArray::class.java)
        val csv = String(response.body!!, StandardCharsets.UTF_8)
        assertThat(csv).contains("Conta")
    }

    @Test
    fun criar_comConta_shouldReturnContaIdNaResposta() {
        val contaResp = restTemplate.exchange("/contas", HttpMethod.POST, HttpEntity(mapOf("nome" to "Nubank", "saldo" to 1000.0), authHeaders()), Map::class.java)
        val contaId = (contaResp.body!!["id"] as Number).toInt()
        val response = restTemplate.exchange(
            "/transactions", HttpMethod.POST,
            HttpEntity(mapOf("descricao" to "Mercado", "valor" to 200.0, "tipo" to "DESPESA", "contaId" to contaId), authHeaders()),
            TransacaoResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.contaId).isEqualTo(contaId.toLong())
        assertThat(response.body!!.conta).isEqualTo("Nubank")
    }
}
