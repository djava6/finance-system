package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.CategoriaRequest;
import br.com.useinet.finance.dto.CategoriaResponse;
import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.repository.CategoriaRepository;
import br.com.useinet.finance.repository.TransacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Gerenciamento de categorias de transações")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;

    public CategoriaController(CategoriaRepository categoriaRepository,
                               TransacaoRepository transacaoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @GetMapping
    @Operation(summary = "Listar categorias")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(
                categoriaRepository.findAll().stream()
                        .map(CategoriaResponse::from)
                        .toList()
        );
    }

    @PostMapping
    @Operation(summary = "Criar categoria")
    public ResponseEntity<CategoriaResponse> criar(@RequestBody CategoriaRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        }
        if (categoriaRepository.findByNome(request.getNome()).isPresent()) {
            throw new IllegalArgumentException("Categoria já existe.");
        }
        Categoria categoria = new Categoria();
        categoria.setNome(request.getNome().trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CategoriaResponse.from(categoriaRepository.save(categoria)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomear categoria")
    public ResponseEntity<CategoriaResponse> renomear(@PathVariable Long id,
                                                       @RequestBody CategoriaRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        }
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        if (categoriaRepository.findByNome(request.getNome()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
        }
        categoria.setNome(request.getNome().trim());
        return ResponseEntity.ok(CategoriaResponse.from(categoriaRepository.save(categoria)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria", description = "Remove uma categoria que não esteja em uso")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        if (transacaoRepository.existsByCategoria(categoria)) {
            throw new IllegalArgumentException("Categoria em uso por transações e não pode ser excluída.");
        }
        categoriaRepository.delete(categoria);
        return ResponseEntity.noContent().build();
    }
}
