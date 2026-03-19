package br.com.useinet.finance.dto

import br.com.useinet.finance.model.BillingEvent
import java.math.BigDecimal
import java.time.OffsetDateTime

data class BillingEventResponse(
    val id: Long,
    val eventType: String,
    val service: String,
    val reason: String?,
    val budgetPct: BigDecimal?,
    val costUsd: BigDecimal?,
    val triggeredBy: String?,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(e: BillingEvent) = BillingEventResponse(
            id          = e.id!!,
            eventType   = e.eventType!!,
            service     = e.service!!,
            reason      = e.reason,
            budgetPct   = e.budgetPct,
            costUsd     = e.costUsd,
            triggeredBy = e.triggeredBy,
            createdAt   = e.createdAt!!
        )
    }
}
