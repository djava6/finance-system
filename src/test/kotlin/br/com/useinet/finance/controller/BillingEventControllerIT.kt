package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.BillingEventResponse
import br.com.useinet.finance.model.BillingEvent
import br.com.useinet.finance.repository.BillingEventRepository
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
import java.math.BigDecimal
import java.time.OffsetDateTime

class BillingEventControllerIT : IntegrationTestBase() {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var billingEventRepository: BillingEventRepository

    companion object {
        const val MOCK_TOKEN = "mock-billing-token"
    }

    @BeforeEach
    fun setUp() {
        billingEventRepository.deleteAll()
        val mockToken = mock(FirebaseToken::class.java)
        `when`(mockToken.uid).thenReturn("uid-billing")
        `when`(mockToken.email).thenReturn("billing@it.com")
        `when`(mockToken.name).thenReturn("Billing User")
        `when`(firebaseAuth.verifyIdToken(eq(MOCK_TOKEN))).thenReturn(mockToken)
    }

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(MOCK_TOKEN) }

    @Test
    fun listar_shouldReturn401WithoutToken() {
        val response = restTemplate.getForEntity(
            "/billing-events?inicio=2025-01-01T00:00:00Z&fim=2025-12-31T23:59:59Z",
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun listar_shouldReturnEmptyListWhenNoEvents() {
        val response = restTemplate.exchange(
            "/billing-events?inicio=2025-01-01T00:00:00Z&fim=2025-12-31T23:59:59Z",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Array<BillingEventResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun listar_shouldReturnEventsWithinDateRange() {
        billingEventRepository.save(BillingEvent().apply {
            eventType = "ALERT"
            service = "Cloud Run"
            reason = "Budget exceeded"
            budgetPct = BigDecimal("75.00")
            costUsd = BigDecimal("150.00")
            triggeredBy = "billing-alert-function"
            createdAt = OffsetDateTime.parse("2025-06-15T10:00:00Z")
        })

        val response = restTemplate.exchange(
            "/billing-events?inicio=2025-06-01T00:00:00Z&fim=2025-06-30T23:59:59Z",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Array<BillingEventResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
        assertThat(response.body!![0].eventType).isEqualTo("ALERT")
        assertThat(response.body!![0].service).isEqualTo("Cloud Run")
        assertThat(response.body!![0].budgetPct).isEqualByComparingTo(BigDecimal("75.00"))
        assertThat(response.body!![0].costUsd).isEqualByComparingTo(BigDecimal("150.00"))
        assertThat(response.body!![0].triggeredBy).isEqualTo("billing-alert-function")
    }

    @Test
    fun listar_shouldNotReturnEventsOutsideDateRange() {
        billingEventRepository.save(BillingEvent().apply {
            eventType = "SHUTDOWN"
            service = "Cloud SQL"
            createdAt = OffsetDateTime.parse("2024-12-01T10:00:00Z")
        })

        val response = restTemplate.exchange(
            "/billing-events?inicio=2025-01-01T00:00:00Z&fim=2025-12-31T23:59:59Z",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Array<BillingEventResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun listar_shouldReturnEventsInDescendingOrder() {
        billingEventRepository.save(BillingEvent().apply {
            eventType = "ALERT"
            service = "Cloud Run"
            createdAt = OffsetDateTime.parse("2025-03-01T08:00:00Z")
        })
        billingEventRepository.save(BillingEvent().apply {
            eventType = "SHUTDOWN"
            service = "Cloud SQL"
            createdAt = OffsetDateTime.parse("2025-03-15T08:00:00Z")
        })

        val response = restTemplate.exchange(
            "/billing-events?inicio=2025-01-01T00:00:00Z&fim=2025-12-31T23:59:59Z",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Array<BillingEventResponse>::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(2)
        // descending order: most recent first
        assertThat(response.body!![0].eventType).isEqualTo("SHUTDOWN")
        assertThat(response.body!![1].eventType).isEqualTo("ALERT")
    }
}
