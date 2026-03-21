package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.DashboardResponse
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

class DashboardControllerIT : IntegrationTestBase() {

    @Autowired lateinit var restTemplate: TestRestTemplate

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
        const val OTHER_TOKEN = "mock-other-token"
    }

    @BeforeEach
    fun setUp() {
        cleanupUser("dashboarduser@it.com")
        cleanupUser("other_dash@it.com")

        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-dashboard")
        `when`(mockToken.email).thenReturn("dashboarduser@it.com")
        `when`(mockToken.name).thenReturn("Dashboard User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)

        val otherToken = mock(FirebaseToken::class.java)
        `when`(otherToken.uid).thenReturn("uid-other-dash")
        `when`(otherToken.email).thenReturn("other_dash@it.com")
        `when`(otherToken.name).thenReturn("Other")
        `when`(firebaseAuth.verifyIdToken(eq(OTHER_TOKEN))).thenReturn(otherToken)
    }

    private fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }
    private fun otherAuthHeaders() = HttpHeaders().apply { setBearerAuth(OTHER_TOKEN) }

    @Test
    fun getDashboard_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/dashboard", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getDashboard_shouldReturn200WithZeroesForNewUser() {
        val response = restTemplate.exchange("/dashboard", HttpMethod.GET, HttpEntity<Any>(authHeaders()), DashboardResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.saldo).isEqualTo(0.0)
        assertThat(response.body!!.totalReceitas).isEqualTo(0.0)
        assertThat(response.body!!.totalDespesas).isEqualTo(0.0)
        assertThat(response.body!!.ultimasTransacoes).isEmpty()
        assertThat(response.body!!.despesasPorCategoria).isEmpty()
        assertThat(response.body!!.contas).isEmpty()
    }

    @Test
    fun getDashboard_shouldReflectCreatedTransactions() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "Salário", "valor" to 5000.0, "tipo" to "RECEITA"), authHeaders()), Any::class.java)
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "Aluguel", "valor" to 1500.0, "tipo" to "DESPESA"), authHeaders()), Any::class.java)

        val response = restTemplate.exchange("/dashboard", HttpMethod.GET, HttpEntity<Any>(authHeaders()), DashboardResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.totalReceitas).isEqualTo(5000.0)
        assertThat(response.body!!.totalDespesas).isEqualTo(1500.0)
        assertThat(response.body!!.saldo).isEqualTo(3500.0)
        assertThat(response.body!!.ultimasTransacoes).hasSize(2)
    }

    @Test
    fun getDashboard_shouldIncludeContas() {
        restTemplate.exchange("/contas", HttpMethod.POST, HttpEntity(mapOf("nome" to "Nubank", "saldo" to 2000.0), authHeaders()), Any::class.java)

        val response = restTemplate.exchange("/dashboard", HttpMethod.GET, HttpEntity<Any>(authHeaders()), DashboardResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.contas).hasSize(1)
        assertThat(response.body!!.contas[0].nome).isEqualTo("Nubank")
        assertThat(response.body!!.contas[0].saldo).isEqualTo(2000.0)
    }

    @Test
    fun getDashboard_shouldNotShowOtherUsersData() {
        restTemplate.exchange("/transactions", HttpMethod.POST, HttpEntity(mapOf("descricao" to "Other income", "valor" to 9999.0, "tipo" to "RECEITA"), otherAuthHeaders()), Any::class.java)

        val response = restTemplate.exchange("/dashboard", HttpMethod.GET, HttpEntity<Any>(authHeaders()), DashboardResponse::class.java)
        assertThat(response.body!!.totalReceitas).isEqualTo(0.0)
        assertThat(response.body!!.ultimasTransacoes).isEmpty()
    }
}
