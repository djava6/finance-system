package br.com.useinet.finance.model

import jakarta.persistence.*

@Entity
@Table(name = "categorias")
open class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true)
    var nome: String? = null
}