package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.ContaRequest;
import br.com.useinet.finance.dto.ContaResponse;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contas")
@Tag(name = "Contas", description = "Gerenciamento de contas bancárias do usuário")
@SecurityRequirement(name = "Bearer Authentication")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping
    @Operation(summary = "Listar contas do usuário")
    public ResponseEntity<List<ContaResponse>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.listar(usuario));
    }

    @PostMapping
    @Operation(summary = "Criar conta")
    public ResponseEntity<ContaResponse> criar(@RequestBody ContaRequest request,
                                               @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contaService.criar(request, usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar conta")
    public ResponseEntity<ContaResponse> atualizar(@PathVariable Long id,
                                                   @RequestBody ContaRequest request,
                                                   @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.atualizar(id, request, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir conta")
    public ResponseEntity<Void> deletar(@PathVariable Long id,
                                        @AuthenticationPrincipal Usuario usuario) {
        contaService.deletar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
