package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.OrcamentoRequest
import br.com.useinet.finance.dto.OrcamentoResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.service.OrcamentoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orçamentos", description = "Gerenciamento de orçamentos mensais por categoria")
@SecurityRequirement(name = "Bearer Authentication")
class OrcamentoController(private val orcamentoService: OrcamentoService) {

    @GetMapping
    @Operation(summary = "Listar orçamentos", description = "Lista todos os orçamentos do usuário")
    fun listar(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<List<OrcamentoResponse>> =
        ResponseEntity.ok(orcamentoService.listar(usuario))

    @GetMapping("/mes")
    @Operation(summary = "Listar por mês", description = "Lista orçamentos de um mês/ano específico")
    fun listarPorMes(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam mes: Int,
        @RequestParam ano: Int
    ): ResponseEntity<List<OrcamentoResponse>> =
        ResponseEntity.ok(orcamentoService.listarPorMes(usuario, mes, ano))

    @PostMapping
    @Operation(summary = "Criar orçamento", description = "Cria um novo orçamento mensal por categoria")
    fun criar(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestBody request: OrcamentoRequest
    ): ResponseEntity<OrcamentoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.criar(request, usuario))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar orçamento", description = "Atualiza o valor limite de um orçamento")
    fun atualizar(
        @AuthenticationPrincipal usuario: Usuario,
        @PathVariable id: Long,
        @RequestBody request: OrcamentoRequest
    ): ResponseEntity<OrcamentoResponse> =
        ResponseEntity.ok(orcamentoService.atualizar(id, request, usuario))

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover orçamento", description = "Remove um orçamento")
    fun deletar(
        @AuthenticationPrincipal usuario: Usuario,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        orcamentoService.deletar(id, usuario)
        return ResponseEntity.noContent().build()
    }
}
