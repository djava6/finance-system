package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByEmail(email: String): Optional<Usuario>
    fun existsByEmail(email: String): Boolean
    fun findByProviderAndProviderId(provider: String, providerId: String): Optional<Usuario>
}
