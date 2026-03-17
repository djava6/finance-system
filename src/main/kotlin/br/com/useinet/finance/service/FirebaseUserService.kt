package br.com.useinet.finance.service

import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FirebaseUserService(private val usuarioRepository: UsuarioRepository) {

    @Transactional
    fun findOrCreate(token: FirebaseToken): Usuario {
        val uid = token.uid
        val email = token.email
        val name = token.name

        return usuarioRepository.findByProviderAndProviderId("firebase", uid)
            .orElseGet {
                usuarioRepository.findByEmail(email)
                    .map { existing ->
                        existing.provider = "firebase"
                        existing.providerId = uid
                        usuarioRepository.save(existing)
                    }
                    .orElseGet {
                        val novo = Usuario().apply {
                            this.nome = name ?: email
                            this.email = email
                            this.provider = "firebase"
                            this.providerId = uid
                        }
                        usuarioRepository.save(novo)
                    }
            }
    }
}
