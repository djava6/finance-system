package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Usuario

data class UserProfileResponse(val id: Long?, val nome: String?, val email: String?) {
    companion object {
        fun from(u: Usuario) = UserProfileResponse(id = u.id, nome = u.nome, email = u.email)
    }
}
