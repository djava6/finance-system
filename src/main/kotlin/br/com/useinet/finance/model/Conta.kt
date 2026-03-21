package br.com.useinet.finance.model

import br.com.useinet.finance.config.AesAttributeConverter
import jakarta.persistence.*

@Entity
@Table(name = "contas")
open class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var nome: String? = null

    var saldo: Double? = null

    @Convert(converter = AesAttributeConverter::class)
    @Column(name = "numero_conta", length = 512)
    var numeroConta: String? = null

    @Convert(converter = AesAttributeConverter::class)
    @Column(length = 512)
    var agencia: String? = null

    @ManyToOne
    var usuario: Usuario? = null
}
