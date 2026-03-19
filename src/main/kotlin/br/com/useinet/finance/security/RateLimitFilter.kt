package br.com.useinet.finance.security

import br.com.useinet.finance.model.Usuario
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val defaultBuckets = ConcurrentHashMap<String, Bucket>()
    private val exportBuckets  = ConcurrentHashMap<String, Bucket>()

    private fun defaultBucket(userId: String): Bucket =
        defaultBuckets.getOrPut(userId) {
            Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
                .build()
        }

    private fun exportBucket(userId: String): Bucket =
        exportBuckets.getOrPut(userId) {
            Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build()
        }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val userId = (SecurityContextHolder.getContext().authentication?.principal as? Usuario)
            ?.providerId ?: "anonymous"

        val isExport = request.requestURI.endsWith("/export")
        val bucket = if (isExport) exportBucket(userId) else defaultBucket(userId)

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            response.status = 429
            response.addHeader("Retry-After", "60")
            response.contentType = "application/json"
            response.writer.write("""{"error":"Too Many Requests","retryAfter":60}""")
        }
    }
}
