package br.com.useinet.finance.model

import jakarta.persistence.*

@Entity
@Table(name = "orcamentos")
class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    var categoria: Categoria? = null

    @Column(name = "valor_limite", nullable = false)
    var valorLimite: Double = 0.0

    @Column(nullable = false)
    var mes: Int = 0

    @Column(nullable = false)
    var ano: Int = 0
}
