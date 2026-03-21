package br.com.useinet.finance.model

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "metas")
class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario? = null

    @Column(nullable = false)
    var nome: String? = null

    @Column(name = "valor_alvo", nullable = false)
    var valorAlvo: Double = 0.0

    @Column(name = "valor_atual", nullable = false)
    var valorAtual: Double = 0.0

    var prazo: LocalDate? = null

    @Column(nullable = false)
    var concluida: Boolean = false
}
