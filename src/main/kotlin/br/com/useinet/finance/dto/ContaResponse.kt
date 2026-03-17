package br.com.useinet.finance.dto

import br.com.useinet.finance.model.Conta

data class ContaResponse(val id: Long, val nome: String, val saldo: Double) {
    companion object {
        fun from(c: Conta) = ContaResponse(id = c.id!!, nome = c.nome!!, saldo = c.saldo!!)
    }
}
