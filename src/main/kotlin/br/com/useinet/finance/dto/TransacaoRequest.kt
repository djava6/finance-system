package br.com.useinet.finance.dto

import br.com.useinet.finance.model.TipoTransacao
import java.time.LocalDateTime

data class TransacaoRequest(
    val descricao: String? = null,
    val valor: Double? = null,
    val tipo: TipoTransacao? = null,
    val data: LocalDateTime? = null,
    val categoriaId: Long? = null,
    val contaId: Long? = null
)
