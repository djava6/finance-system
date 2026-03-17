package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.ContaRequest
import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.service.ContaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/contas")
@Tag(name = "Contas", description = "Gerenciamento de contas bancárias do usuário")
@SecurityRequirement(name = "Bearer Authentication")
class ContaController(private val contaService: ContaService) {

    @GetMapping
    @Operation(summary = "Listar contas do usuário")
    fun listar(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<List<ContaResponse>> =
        ResponseEntity.ok(contaService.listar(usuario))

    @PostMapping
    @Operation(summary = "Criar conta")
    fun criar(
        @RequestBody request: ContaRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<ContaResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(contaService.criar(request, usuario))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar conta")
    fun atualizar(
        @PathVariable id: Long,
        @RequestBody request: ContaRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<ContaResponse> =
        ResponseEntity.ok(contaService.atualizar(id, request, usuario))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir conta")
    fun deletar(@PathVariable id: Long, @AuthenticationPrincipal usuario: Usuario): ResponseEntity<Void> {
        contaService.deletar(id, usuario)
        return ResponseEntity.noContent().build()
    }
}
