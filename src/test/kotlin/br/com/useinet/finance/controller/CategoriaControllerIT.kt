package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.CategoriaResponse
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
class CategoriaControllerIT {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @MockBean lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val MOCK_TOKEN = "mock-firebase-token"
    }

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM categorias WHERE nome LIKE '% IT'")
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-cat")
        `when`(mockToken.email).thenReturn("catuser@it.com")
        `when`(mockToken.name).thenReturn("Cat User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    @Test
    fun listar_shouldReturn200WithEmptyOrPopulatedList() {
        val response = restTemplate.exchange("/categories", HttpMethod.GET, HttpEntity<Any>(authHeaders()), Array<CategoriaResponse>::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
    }

    @Test
    fun criar_shouldReturn201WithNewCategory() {
        val response = restTemplate.postForEntity(
            "/categories",
            HttpEntity(mapOf("nome" to "Alimentação IT"), authHeaders()),
            CategoriaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.nome).isEqualTo("Alimentação IT")
    }

    @Test
    fun criar_shouldReturn400WhenNomeIsBlank() {
        val response = restTemplate.postForEntity(
            "/categories",
            HttpEntity(mapOf("nome" to ""), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun criar_shouldReturn400WhenCategoriaJaExiste() {
        val body = mapOf("nome" to "Duplicada IT")
        restTemplate.postForEntity("/categories", HttpEntity(body, authHeaders()), CategoriaResponse::class.java)
        val response = restTemplate.postForEntity("/categories", HttpEntity(body, authHeaders()), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun deletar_shouldReturn204WhenCategoryExists() {
        val created = restTemplate.postForEntity(
            "/categories",
            HttpEntity(mapOf("nome" to "Para Deletar IT"), authHeaders()),
            CategoriaResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange("/categories/$id", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), Void::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity("/categories", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun renomear_shouldReturn200WithNewName() {
        val created = restTemplate.postForEntity(
            "/categories",
            HttpEntity(mapOf("nome" to "Original IT"), authHeaders()),
            CategoriaResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange(
            "/categories/$id", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to "Renomeada IT"), authHeaders()),
            CategoriaResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.nome).isEqualTo("Renomeada IT")
    }

    @Test
    fun renomear_shouldReturn400WhenNomeIsBlank() {
        val created = restTemplate.postForEntity(
            "/categories",
            HttpEntity(mapOf("nome" to "Para Renomear Blank IT"), authHeaders()),
            CategoriaResponse::class.java
        )
        val id = created.body!!.id
        val response = restTemplate.exchange(
            "/categories/$id", HttpMethod.PUT,
            HttpEntity(mapOf("nome" to ""), authHeaders()),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
