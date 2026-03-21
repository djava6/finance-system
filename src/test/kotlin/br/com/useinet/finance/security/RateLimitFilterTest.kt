package br.com.useinet.finance.security

import br.com.useinet.finance.model.Usuario
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class RateLimitFilterTest {

    private val filter = RateLimitFilter()

    private fun usuario(providerId: String) = Usuario().apply {
        nome = "Test"; email = "$providerId@test.com"; this.providerId = providerId
    }

    private fun authenticate(providerId: String) {
        val user = usuario(providerId)
        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun doRequest(uri: String = "/transactions"): MockHttpServletResponse {
        val request = MockHttpServletRequest("GET", uri)
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)
        filter.doFilter(request, response, chain)
        return response
    }

    @Test
    fun shouldPassThroughWhenUnderLimit() {
        authenticate("user-pass")
        val response = doRequest()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun shouldReturn429AfterSixtyDefaultRequests() {
        authenticate("user-60limit")
        repeat(60) { doRequest() }
        val response = doRequest()
        assertThat(response.status).isEqualTo(429)
    }

    @Test
    fun shouldReturnRetryAfterHeaderOn429() {
        authenticate("user-retry-after")
        repeat(60) { doRequest() }
        val response = doRequest()
        assertThat(response.getHeader("Retry-After")).isEqualTo("60")
    }

    @Test
    fun shouldReturnJsonBodyOn429() {
        authenticate("user-body")
        repeat(60) { doRequest() }
        val response = doRequest()
        assertThat(response.contentAsString).contains("Too Many Requests")
        assertThat(response.contentAsString).contains("retryAfter")
        assertThat(response.contentType).contains("application/json")
    }

    @Test
    fun shouldNotCallFilterChainWhenLimitExceeded() {
        authenticate("user-chain")
        repeat(60) { doRequest() }

        val request = MockHttpServletRequest("GET", "/transactions")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)
        filter.doFilter(request, response, chain)

        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun shouldUseSeparateBucketsPerUser() {
        authenticate("user-a")
        repeat(60) { doRequest() }
        val responseA = doRequest()
        assertThat(responseA.status).isEqualTo(429)

        // User B has a fresh bucket — should still pass
        SecurityContextHolder.clearContext()
        authenticate("user-b")
        val responseB = doRequest()
        assertThat(responseB.status).isEqualTo(200)
    }

    @Test
    fun shouldUseAnonymousBucketWhenNotAuthenticated() {
        // no authentication set up
        val response = doRequest()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun exportEndpoint_shouldUseExportBucketWithCapacityFive() {
        authenticate("user-export")
        repeat(5) { doRequest("/transactions/export") }
        val response = doRequest("/transactions/export")
        assertThat(response.status).isEqualTo(429)
    }

    @Test
    fun exportCsvEndpoint_doesNotTriggerExportBucket() {
        // /transactions/export/csv does NOT end with "/export", so uses default bucket (60/min)
        authenticate("user-export-csv")
        repeat(5) { doRequest("/transactions/export/csv") }
        val response = doRequest("/transactions/export/csv")
        // Still 200 — export/csv uses the default bucket, not the 5/min export bucket
        assertThat(response.status).isEqualTo(200)
    }
}
