package br.com.useinet.finance.service

import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CsvExportTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var categoriaRepository: CategoriaRepository
    @Mock lateinit var contaRepository: ContaRepository
    @Mock lateinit var orcamentoService: OrcamentoService
    @InjectMocks lateinit var transacaoService: TransacaoService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    private fun exportAll(usuario: Usuario) = transacaoService.exportarCsv(usuario, null, null)
    private fun exportPeriod(usuario: Usuario, inicio: LocalDate, fim: LocalDate) =
        transacaoService.exportarCsv(usuario, inicio.atStartOfDay(), fim.atTime(23, 59, 59))

    @Test
    fun exportarCsv_shouldReturnBomBytes() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())

        val csv = exportAll(usuario)
        assertThat(csv[0]).isEqualTo(0xEF.toByte())
        assertThat(csv[1]).isEqualTo(0xBB.toByte())
        assertThat(csv[2]).isEqualTo(0xBF.toByte())
    }

    @Test
    fun exportarCsv_shouldContainResumoHeader() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("Período:")
        assertThat(content).contains("Total Receitas:")
        assertThat(content).contains("Total Despesas:")
        assertThat(content).contains("Saldo:")
        assertThat(content).contains("Todas as transações")
    }

    @Test
    fun exportarCsv_shouldUseSemicolonAsDelimiter() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("ID;Descrição;Valor;Tipo;Data;Categoria;Conta;\"Saldo da Conta\"")
        assertThat(content).doesNotContain("ID,")
    }

    @Test
    fun exportarCsv_shouldFormatValorWithCommaDecimal() {
        val usuario = usuarioMock()
        val t = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 5000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("5000,00")
        assertThat(content).doesNotContain("5000.00")
    }

    @Test
    fun exportarCsv_shouldUseLegibleTipoNames() {
        val usuario = usuarioMock()
        val receita = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 1000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        val despesa = Transacao().apply {
            id = 2L; descricao = "Almoço"; valor = 25.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 15, 12, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(receita, despesa))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("Receita")
        assertThat(content).contains("Despesa")
        assertThat(content).doesNotContain("RECEITA")
        assertThat(content).doesNotContain("DESPESA")
    }

    @Test
    fun exportarCsv_shouldIncludeSaldoDaConta() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 1L; nome = "Nubank"; saldo = 1975.0 }
        val t = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 1000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
            this.conta = conta
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))
        // saldo acumulado calculado pela soma das transações da conta (ASC), não pelo campo conta.saldo
        `when`(transacaoRepository.findByContaOrderByDataAsc(conta)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        // running balance após única RECEITA 1000,00 = 1000,00
        assertThat(content).contains("Nubank;1000,00")
    }

    @Test
    fun exportarCsv_shouldCalculateResumoCorrectly() {
        val usuario = usuarioMock()
        val receita = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 1000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        val despesa = Transacao().apply {
            id = 2L; descricao = "Almoço"; valor = 25.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 15, 12, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(receita, despesa))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains(";1000,00")
        assertThat(content).contains(";25,00")
        assertThat(content).contains(";975,00")
    }

    @Test
    fun exportarCsv_shouldFilterByPeriodAndShowPeriodInResumo() {
        val usuario = usuarioMock()
        val inicio = LocalDate.of(2026, 3, 1)
        val fim = LocalDate.of(2026, 3, 31)
        val t = Transacao().apply {
            id = 1L; descricao = "Dentro"; valor = 500.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        `when`(transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(
            usuario, inicio.atStartOfDay(), fim.atTime(23, 59, 59)
        )).thenReturn(listOf(t))

        val content = String(exportPeriod(usuario, inicio, fim), StandardCharsets.UTF_8)
        assertThat(content).contains("01/03/2026 a 31/03/2026")
        assertThat(content).contains("Dentro")
    }

    @Test
    fun exportarCsv_shouldIncludeCategoryName() {
        val usuario = usuarioMock()
        val categoria = Categoria().apply { id = 1L; nome = "Alimentação" }
        val t = Transacao().apply {
            id = 2L; descricao = "Mercado"; valor = 200.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 15, 12, 0)
            this.categoria = categoria
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("Alimentação")
    }

    @Test
    fun exportarCsv_shouldEscapeSemicolonsInDescricao() {
        val usuario = usuarioMock()
        val t = Transacao().apply {
            id = 3L; descricao = "Salário; bônus"; valor = 100.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 8, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("\"Salário; bônus\"")
    }

    // ─── importarCsv ──────────────────────────────────────────────────────────

    /** Simple import format (no summary lines, positional headers) */
    private fun csv(vararg rows: String): MockMultipartFile {
        val header = "data,descricao,valor,tipo,categoria"
        val content = (listOf(header) + rows).joinToString("\n")
        return MockMultipartFile("file", "import.csv", "text/csv", content.toByteArray(Charsets.UTF_8))
    }

    /** Export-format CSV (BOM + summary lines + semicolon separator + dd/MM/yyyy HH:mm dates) */
    private fun exportCsv(vararg rows: String): MockMultipartFile {
        val summary = listOf(
            "\uFEFFPeríodo:;Todas as transações",
            "Total Receitas:;1000,00",
            "Total Despesas:;0,00",
            "Saldo:;1000,00",
            "",
            "ID;Descrição;Valor;Tipo;Data;Categoria;Conta;Saldo da Conta"
        )
        val content = (summary + rows).joinToString("\n")
        return MockMultipartFile("file", "export.csv", "text/csv", content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun importarCsv_shouldImportValidRows() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val file = csv("2025-01-15,Salário,5000.0,RECEITA,", "2025-01-20,Mercado,200.0,DESPESA,")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(2)
        assertThat(result.duplicatas).isEqualTo(0)
        assertThat(result.erros).isEqualTo(0)
    }

    @Test
    fun importarCsv_shouldSkipDuplicates() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(true)

        val file = csv("2025-01-15,Salário,5000.0,RECEITA,")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(0)
        assertThat(result.duplicatas).isEqualTo(1)
    }

    @Test
    fun importarCsv_shouldCountErrorsForInvalidRows() {
        val usuario = usuarioMock()
        // invalid tipo + invalid date — neither reaches existsByUsuario... check, so no stub needed
        val file = csv("2025-01-15,Salário,5000.0,INVALIDO,", "nao-e-data,Mercado,200.0,DESPESA,")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.erros).isEqualTo(2)
        assertThat(result.mensagensErro).hasSize(2)
        assertThat(result.mensagensErro[0]).contains("Linha 2")
        assertThat(result.mensagensErro[1]).contains("Linha 3")
    }

    @Test
    fun importarCsv_shouldAssociateCategoryByName() {
        val usuario = usuarioMock()
        val cat = Categoria().apply { id = 1L; nome = "Alimentação" }
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(categoriaRepository.findByNome("Alimentação")).thenReturn(Optional.of(cat))
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val file = csv("2025-01-20,Mercado,200.0,DESPESA,Alimentação")
        transacaoService.importarCsv(file, usuario)

        verify(categoriaRepository).findByNome("Alimentação")
    }

    @Test
    fun importarCsv_shouldTreatNegativeValorAsAbsolute() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        val saved = mutableListOf<Transacao>()
        `when`(transacaoRepository.save(any())).thenAnswer { inv ->
            val t = inv.getArgument<Transacao>(0)
            saved.add(t)
            t
        }

        val file = csv("2025-01-20,Compra,-150.0,DESPESA,")
        transacaoService.importarCsv(file, usuario)

        assertThat(saved).hasSize(1)
        assertThat(saved[0].valor).isEqualTo(150.0)
    }

    @Test
    fun importarCsv_shouldAcceptLowercaseTipo() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        // service calls .uppercase() on tipo, so lowercase should parse fine
        val file = csv("2025-01-15,Salário,5000.0,receita,")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(1)
        assertThat(result.erros).isEqualTo(0)
    }

    @Test
    fun importarCsv_shouldSaveWithoutCategoryWhenNotFoundInDb() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(categoriaRepository.findByNome("Inexistente")).thenReturn(Optional.empty())
        val saved = mutableListOf<Transacao>()
        `when`(transacaoRepository.save(any())).thenAnswer { inv ->
            val t = inv.getArgument<Transacao>(0)
            saved.add(t)
            t
        }

        val file = csv("2025-01-20,Mercado,200.0,DESPESA,Inexistente")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(1)
        assertThat(result.erros).isEqualTo(0)
        assertThat(saved[0].categoria).isNull()
    }

    @Test
    fun importarCsv_shouldIgnoreBlankCategoryColumn() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val file = csv("2025-01-20,Mercado,200.0,DESPESA,")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(1)
        // findByNome should NOT be called for blank category
        verify(categoriaRepository, never()).findByNome(any())
    }

    // ─── export → import round-trip ───────────────────────────────────────────

    @Test
    fun importarCsv_shouldImportExportFormatWithBomAndSummaryLines() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        // Rows in the export column order: ID;Descrição;Valor;Tipo;Data;Categoria;Conta;Saldo da Conta
        val file = exportCsv(
            "1;Salário;5000,00;Receita;15/03/2026 10:00;;Nubank;5000,00",
            "2;Mercado;200,00;Despesa;15/03/2026 12:00;Alimentação;Nubank;4800,00"
        )
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.importadas).isEqualTo(2)
        assertThat(result.duplicatas).isEqualTo(0)
        assertThat(result.erros).isEqualTo(0)
        assertThat(result.mensagensErro).isEmpty()
    }

    @Test
    fun importarCsv_shouldSkipSummaryLinesAndNotCountThemAsErrors() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val file = exportCsv("1;Pagamento;100,00;Despesa;20/03/2026 09:00;;Carteira;900,00")
        val result = transacaoService.importarCsv(file, usuario)

        assertThat(result.erros).isEqualTo(0)
        assertThat(result.importadas).isEqualTo(1)
    }

    @Test
    fun importarCsv_shouldParseDateInExportFormat() {
        val usuario = usuarioMock()
        val saved = mutableListOf<Transacao>()
        `when`(transacaoRepository.existsByUsuarioAndDataAndValorAndDescricao(any(), any(), any(), any())).thenReturn(false)
        `when`(transacaoRepository.save(any())).thenAnswer { inv ->
            val t = inv.getArgument<Transacao>(0)
            saved.add(t)
            t
        }

        val file = exportCsv("3;Almoço;35,50;Despesa;21/03/2026 13:30;;Carteira;")
        transacaoService.importarCsv(file, usuario)

        assertThat(saved).hasSize(1)
        assertThat(saved[0].data).isEqualTo(java.time.LocalDateTime.of(2026, 3, 21, 13, 30))
        assertThat(saved[0].valor).isEqualTo(35.5)
    }
}
