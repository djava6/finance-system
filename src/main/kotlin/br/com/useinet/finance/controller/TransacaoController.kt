package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.service.TransacaoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Gerenciamento de receitas e despesas")
@SecurityRequirement(name = "Bearer Authentication")
class TransacaoController(private val transacaoService: TransacaoService) {

    @PostMapping
    @Operation(summary = "Criar transação", description = "Registra uma nova receita ou despesa")
    fun criar(
        @RequestBody request: TransacaoRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<TransacaoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(transacaoService.criar(request, usuario))

    @GetMapping
    @Operation(summary = "Listar transações", description = "Retorna transações do usuário. Filtro opcional por período (inicio/fim no formato yyyy-MM-dd)")
    fun listar(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) inicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fim: LocalDate?
    ): ResponseEntity<List<TransacaoResponse>> {
        val dtInicio = inicio?.atStartOfDay()
        val dtFim = fim?.atTime(23, 59, 59)

        if (dtInicio != null && dtFim != null && dtInicio.isAfter(dtFim)) {
            throw IllegalArgumentException("Data inicial deve ser anterior à data final.")
        }

        return ResponseEntity.ok(transacaoService.listar(usuario, dtInicio, dtFim))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar transação", description = "Atualiza descrição, valor, tipo, categoria de uma transação do usuário")
    fun atualizar(
        @PathVariable id: Long,
        @RequestBody request: TransacaoRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<TransacaoResponse> =
        ResponseEntity.ok(transacaoService.atualizar(id, request, usuario))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir transação", description = "Remove uma transação do usuário logado")
    fun deletar(@PathVariable id: Long, @AuthenticationPrincipal usuario: Usuario): ResponseEntity<Void> {
        transacaoService.deletar(id, usuario)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Exportar CSV", description = "Baixa todas as transações do usuário em formato CSV")
    fun exportarCsv(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<ByteArray> {
        val csv = transacaoService.exportarCsv(usuario)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transacoes.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv)
    }
}
