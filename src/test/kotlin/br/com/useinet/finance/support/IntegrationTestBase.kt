package br.com.useinet.finance.support

import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @MockBean protected lateinit var firebaseAuth: FirebaseAuth
    @Autowired protected lateinit var jdbcTemplate: JdbcTemplate

    protected fun cleanupUser(email: String) {
        jdbcTemplate.update("DELETE FROM transacoes WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM contas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = ?)", email)
        jdbcTemplate.update("DELETE FROM usuarios WHERE email = ?", email)
    }

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
