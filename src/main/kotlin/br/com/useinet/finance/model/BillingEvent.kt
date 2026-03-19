package br.com.useinet.finance.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "billing_events")
open class BillingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "event_type", nullable = false)
    var eventType: String? = null

    @Column(nullable = false)
    var service: String? = null

    var reason: String? = null

    @Column(name = "budget_pct", columnDefinition = "numeric(5,2)")
    var budgetPct: BigDecimal? = null

    @Column(name = "cost_usd", columnDefinition = "numeric(10,2)")
    var costUsd: BigDecimal? = null

    @Column(name = "triggered_by")
    var triggeredBy: String? = null

    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null
}
