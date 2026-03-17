package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Categoria

data class CategoriaResponse(val id: Long, val nome: String) {
    companion object {
        fun from(c: Categoria) = CategoriaResponse(id = c.id!!, nome = c.nome!!)
    }
}
