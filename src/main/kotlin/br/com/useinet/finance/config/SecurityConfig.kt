package br.com.useinet.finance.config

import br.com.useinet.finance.security.FirebaseAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        firebaseAuthenticationFilter: FirebaseAuthenticationFilter
    ): SecurityFilterChain {
        http
            .cors { cors ->
                cors.configurationSource {
                    CorsConfiguration().apply {
                        allowedOriginPatterns = listOf("*")
                        allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        allowedHeaders = listOf("*")
                        allowCredentials = true
                    }
                }
            }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, res, _ ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                }
            }
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
