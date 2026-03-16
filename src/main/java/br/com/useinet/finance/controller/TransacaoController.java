package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.TransacaoRequest;
import br.com.useinet.finance.dto.TransacaoResponse;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Gerenciamento de receitas e despesas")
@SecurityRequirement(name = "Bearer Authentication")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    @Operation(summary = "Criar transação", description = "Registra uma nova receita ou despesa")
    public ResponseEntity<TransacaoResponse> criar(@RequestBody TransacaoRequest request,
                                                   @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transacaoService.criar(request, usuario));
    }

    @GetMapping
    @Operation(summary = "Listar transações", description = "Retorna transações do usuário. Filtro opcional por período (inicio/fim no formato yyyy-MM-dd)")
    public ResponseEntity<List<TransacaoResponse>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        LocalDateTime dtInicio = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime dtFim = fim != null ? fim.atTime(23, 59, 59) : null;

        if (dtInicio != null && dtFim != null && dtInicio.isAfter(dtFim)) {
            throw new IllegalArgumentException("Data inicial deve ser anterior à data final.");
        }

        return ResponseEntity.ok(transacaoService.listar(usuario, dtInicio, dtFim));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar transação", description = "Atualiza descrição, valor, tipo, categoria de uma transação do usuário")
    public ResponseEntity<TransacaoResponse> atualizar(@PathVariable Long id,
                                                       @RequestBody TransacaoRequest request,
                                                       @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.atualizar(id, request, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir transação", description = "Remove uma transação do usuário logado")
    public ResponseEntity<Void> deletar(@PathVariable Long id,
                                        @AuthenticationPrincipal Usuario usuario) {
        transacaoService.deletar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Exportar CSV", description = "Baixa todas as transações do usuário em formato CSV")
    public ResponseEntity<byte[]> exportarCsv(@AuthenticationPrincipal Usuario usuario) {
        byte[] csv = transacaoService.exportarCsv(usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transacoes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
