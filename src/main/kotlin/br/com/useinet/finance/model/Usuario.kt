package br.com.useinet.finance.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "usuarios")
class Usuario : UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var nome: String? = null

    @Column(unique = true, nullable = false)
    var email: String? = null

    @Column(nullable = true)
    var provider: String? = null

    @Column(name = "provider_id", nullable = true)
    var providerId: String? = null

    @Column(name = "fcm_token")
    var fcmToken: String? = null

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun getPassword(): String? = null
    override fun getUsername(): String? = email
}