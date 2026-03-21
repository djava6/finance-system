package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.MetaDepositoRequest
import br.com.useinet.finance.dto.MetaRequest
import br.com.useinet.finance.dto.MetaResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.service.MetaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/metas")
@Tag(name = "Metas", description = "Gerenciamento de metas financeiras")
@SecurityRequirement(name = "Bearer Authentication")
class MetaController(private val metaService: MetaService) {

    @GetMapping
    @Operation(summary = "Listar metas", description = "Lista todas as metas financeiras do usuário")
    fun listar(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<List<MetaResponse>> =
        ResponseEntity.ok(metaService.listar(usuario))

    @PostMapping
    @Operation(summary = "Criar meta", description = "Cria uma nova meta financeira")
    fun criar(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestBody request: MetaRequest
    ): ResponseEntity<MetaResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(metaService.criar(request, usuario))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar meta", description = "Atualiza os dados de uma meta")
    fun atualizar(
        @AuthenticationPrincipal usuario: Usuario,
        @PathVariable id: Long,
        @RequestBody request: MetaRequest
    ): ResponseEntity<MetaResponse> =
        ResponseEntity.ok(metaService.atualizar(id, request, usuario))

    @PatchMapping("/{id}/deposito")
    @Operation(summary = "Depositar na meta", description = "Adiciona um valor à meta financeira")
    fun depositar(
        @AuthenticationPrincipal usuario: Usuario,
        @PathVariable id: Long,
        @RequestBody request: MetaDepositoRequest
    ): ResponseEntity<MetaResponse> =
        ResponseEntity.ok(metaService.depositar(id, request, usuario))

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover meta", description = "Remove uma meta financeira")
    fun deletar(
        @AuthenticationPrincipal usuario: Usuario,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        metaService.deletar(id, usuario)
        return ResponseEntity.noContent().build()
    }
}
