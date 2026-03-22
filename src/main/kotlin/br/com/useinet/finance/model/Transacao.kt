package br.com.useinet.finance.model

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "transacoes")
class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var descricao: String? = null

    @Column(nullable = false, columnDefinition = "numeric(15,2)")
    var valor: Double? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tipo: TipoTransacao? = null

    @Column(nullable = false)
    var data: LocalDate? = null

    @ManyToOne
    var categoria: Categoria? = null

    @ManyToOne
    var conta: Conta? = null

    @ManyToOne
    var usuario: Usuario? = null

    @Column(nullable = false)
    var recorrente: Boolean = false

    @Enumerated(EnumType.STRING)
    var frequencia: FrequenciaRecorrencia? = null

    var proximaOcorrencia: LocalDate? = null

    @Column(length = 500)
    var reciboUrl: String? = null
}
