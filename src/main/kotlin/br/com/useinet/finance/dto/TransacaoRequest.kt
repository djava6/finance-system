package br.com.useinet.finance.dto

import br.com.useinet.finance.model.FrequenciaRecorrencia
import br.com.useinet.finance.model.TipoTransacao
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class TransacaoRequest(
    @field:NotBlank(message = "Descrição da transação é obrigatória.")
    val descricao: String? = null,

    @field:NotNull(message = "Valor da transação é obrigatório.")
    @field:Positive(message = "Valor da transação deve ser maior que zero.")
    val valor: Double? = null,

    @field:NotNull(message = "Tipo da transação é obrigatório.")
    val tipo: TipoTransacao? = null,

    val data: LocalDate? = null,
    val categoriaId: Long? = null,
    val contaId: Long? = null,
    val recorrente: Boolean = false,
    val frequencia: FrequenciaRecorrencia? = null,
    val proximaOcorrencia: LocalDate? = null
)
