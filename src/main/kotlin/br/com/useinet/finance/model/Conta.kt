package br.com.useinet.finance.model

import jakarta.persistence.*

@Entity
@Table(name = "contas")
open class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var nome: String? = null

    var saldo: Double? = null

    @ManyToOne
    var usuario: Usuario? = null
}
