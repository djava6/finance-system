package br.com.useinet.finance.controller

import br.com.useinet.finance.dto.ImportResultResponse
import br.com.useinet.finance.dto.PageResponse
import br.com.useinet.finance.dto.ReciboUrlRequest
import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.service.TransacaoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Gerenciamento de receitas e despesas")
@SecurityRequirement(name = "Bearer Authentication")
class TransacaoController(private val transacaoService: TransacaoService) {

    @PostMapping
    @Operation(summary = "Criar transação", description = "Registra uma nova receita ou despesa")
    fun criar(
        @Valid @RequestBody request: TransacaoRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<TransacaoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(transacaoService.criar(request, usuario))

    @GetMapping
    @Operation(summary = "Listar transações", description = "Retorna transações paginadas. Filtro opcional por período (inicio/fim no formato yyyy-MM-dd). Padrão: page=0, size=20.")
    fun listar(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) inicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fim: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<TransacaoResponse>> {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw IllegalArgumentException("Data inicial deve ser anterior à data final.")
        }

        val pageable = PageRequest.of(page, size, Sort.by("data").descending())
        return ResponseEntity.ok(transacaoService.listar(usuario, inicio, fim, pageable))
    }

    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Importar CSV", description = "Importa transações de um arquivo CSV. Formato esperado: data,descricao,valor,tipo,categoria")
    fun importarCsv(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<ImportResultResponse> =
        ResponseEntity.ok(transacaoService.importarCsv(file, usuario))

    @PutMapping("/{id}")
    @Operation(summary = "Editar transação", description = "Atualiza descrição, valor, tipo, categoria de uma transação do usuário")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: TransacaoRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<TransacaoResponse> =
        ResponseEntity.ok(transacaoService.atualizar(id, request, usuario))

    @PatchMapping("/{id}/recibo")
    @Operation(summary = "Salvar URL do recibo", description = "Associa a URL do Firebase Storage ao recibo da transação")
    fun atualizarRecibo(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReciboUrlRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<TransacaoResponse> =
        ResponseEntity.ok(transacaoService.atualizarRecibo(id, request, usuario))

    @DeleteMapping("/{id}/recibo")
    @Operation(summary = "Remover recibo", description = "Remove a URL do recibo da transação")
    fun removerRecibo(@PathVariable id: Long, @AuthenticationPrincipal usuario: Usuario): ResponseEntity<Void> {
        transacaoService.removerRecibo(id, usuario)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir transação", description = "Remove uma transação do usuário logado")
    fun deletar(@PathVariable id: Long, @AuthenticationPrincipal usuario: Usuario): ResponseEntity<Void> {
        transacaoService.deletar(id, usuario)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/export/xlsx", produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"])
    @Operation(summary = "Exportar XLSX", description = "Baixa transações do usuário em Excel. Filtro opcional por período (inicio/fim no formato yyyy-MM-dd). Receitas em verde, despesas em vermelho.")
    fun exportarXlsx(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) inicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fim: LocalDate?,
        response: HttpServletResponse
    ) {
        response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"transacoes_${LocalDate.now()}.xlsx\"")
        transacaoService.exportarXlsx(usuario, inicio, fim, response.outputStream)
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Exportar CSV", description = "Baixa transações do usuário em CSV. Filtro opcional por período (inicio/fim no formato yyyy-MM-dd). Inclui resumo financeiro no topo.")
    fun exportarCsv(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) inicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fim: LocalDate?
    ): ResponseEntity<ByteArray> {
        val csv = transacaoService.exportarCsv(usuario, inicio, fim)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transacoes.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv)
    }
}
