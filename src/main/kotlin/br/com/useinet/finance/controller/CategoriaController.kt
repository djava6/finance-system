package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.CategoriaRequest
import br.com.useinet.finance.dto.CategoriaResponse
import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Gerenciamento de categorias de transações")
@SecurityRequirement(name = "Bearer Authentication")
class CategoriaController(
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository
) {

    @GetMapping
    @Operation(summary = "Listar categorias")
    fun listar(): ResponseEntity<List<CategoriaResponse>> =
        ResponseEntity.ok(categoriaRepository.findAll().map { CategoriaResponse.from(it) })

    @PostMapping
    @Operation(summary = "Criar categoria")
    fun criar(@RequestBody request: CategoriaRequest): ResponseEntity<CategoriaResponse> {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da categoria é obrigatório.")
        if (categoriaRepository.findByNome(request.nome).isPresent) throw IllegalArgumentException("Categoria já existe.")
        val categoria = Categoria().apply { this.nome = request.nome.trim() }
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.from(categoriaRepository.save(categoria)))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomear categoria")
    fun renomear(@PathVariable id: Long, @RequestBody request: CategoriaRequest): ResponseEntity<CategoriaResponse> {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da categoria é obrigatório.")
        val categoria = categoriaRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        if (categoriaRepository.findByNome(request.nome).isPresent) throw IllegalArgumentException("Já existe uma categoria com esse nome.")
        categoria.nome = request.nome.trim()
        return ResponseEntity.ok(CategoriaResponse.from(categoriaRepository.save(categoria)))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria", description = "Remove uma categoria que não esteja em uso")
    fun deletar(@PathVariable id: Long): ResponseEntity<Void> {
        val categoria = categoriaRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        if (transacaoRepository.existsByCategoria(categoria)) {
            throw IllegalArgumentException("Categoria em uso por transações e não pode ser excluída.")
        }
        categoriaRepository.delete(categoria)
        return ResponseEntity.noContent().build()
    }
}
