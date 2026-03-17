package br.com.useinet.finance.security

import br.com.useinet.finance.service.FirebaseUserService
import com.google.firebase.auth.FirebaseAuth
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class FirebaseAuthenticationFilter(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseUserService: FirebaseUserService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(FirebaseAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val idToken = authHeader.substring(7)
            try {
                val decodedToken = firebaseAuth.verifyIdToken(idToken)
                val usuario = firebaseUserService.findOrCreate(decodedToken)
                val authentication = UsernamePasswordAuthenticationToken(usuario, null, usuario.authorities)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                log.warn("Firebase token verification failed: {}", e.message)
            }
        }
        chain.doFilter(request, response)
    }
}
