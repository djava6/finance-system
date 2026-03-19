package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.BillingEventResponse
import br.com.useinet.finance.repository.BillingEventRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/billing-events")
@Tag(name = "Billing Events", description = "Histórico de alertas de custo e suspensões automáticas")
@SecurityRequirement(name = "Bearer Authentication")
class BillingEventController(private val repository: BillingEventRepository) {

    @GetMapping
    @Operation(summary = "Listar eventos de billing por período")
    fun listar(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) inicio: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fim: OffsetDateTime
    ): ResponseEntity<List<BillingEventResponse>> =
        ResponseEntity.ok(
            repository.findByCreatedAtBetweenOrderByCreatedAtDesc(inicio, fim)
                .map(BillingEventResponse::from)
        )
}
