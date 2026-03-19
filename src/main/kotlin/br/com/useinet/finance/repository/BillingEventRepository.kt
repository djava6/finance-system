package br.com.useinet.finance.repository

import br.com.useinet.finance.model.BillingEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BillingEventRepository : JpaRepository<BillingEvent, Long> {
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        inicio: OffsetDateTime,
        fim: OffsetDateTime
    ): List<BillingEvent>
}
