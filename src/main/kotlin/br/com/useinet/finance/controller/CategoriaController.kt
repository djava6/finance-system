package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.CategoriaRequest
import br.com.useinet.finance.dto.CategoriaResponse
import br.com.useinet.finance.service.CategoriaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Gerenciamento de categorias de transações")
@SecurityRequirement(name = "Bearer Authentication")
class CategoriaController(private val categoriaService: CategoriaService) {

    @GetMapping
    @Operation(summary = "Listar categorias")
    fun listar(): ResponseEntity<List<CategoriaResponse>> =
        ResponseEntity.ok(categoriaService.listar())

    @PostMapping
    @Operation(summary = "Criar categoria")
    fun criar(@Valid @RequestBody request: CategoriaRequest): ResponseEntity<CategoriaResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(request))

    @PutMapping("/{id}")
    @Operation(summary = "Renomear categoria")
    fun renomear(@PathVariable id: Long, @Valid @RequestBody request: CategoriaRequest): ResponseEntity<CategoriaResponse> =
        ResponseEntity.ok(categoriaService.renomear(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria", description = "Remove uma categoria que não esteja em uso")
    fun deletar(@PathVariable id: Long): ResponseEntity<Void> {
        categoriaService.deletar(id)
        return ResponseEntity.noContent().build()
    }
}
