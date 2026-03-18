package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Conta

data class ContaResponse(val id: Long, val nome: String, val saldo: Double) {
    companion object {
        fun from(c: Conta) = ContaResponse(
            id = requireNotNull(c.id) { "Conta.id não pode ser nulo" },
            nome = requireNotNull(c.nome) { "Conta.nome não pode ser nulo" },
            saldo = requireNotNull(c.saldo) { "Conta.saldo não pode ser nulo" }
        )
    }
}
