package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Categoria

data class CategoriaResponse(val id: Long, val nome: String) {
    companion object {
        fun from(c: Categoria) = CategoriaResponse(
            id = requireNotNull(c.id) { "Categoria.id não pode ser nulo" },
            nome = requireNotNull(c.nome) { "Categoria.nome não pode ser nulo" }
        )
    }
}
