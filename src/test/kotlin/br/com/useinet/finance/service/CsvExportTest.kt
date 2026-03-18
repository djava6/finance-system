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
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CsvExportTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var categoriaRepository: CategoriaRepository
    @Mock lateinit var contaRepository: ContaRepository
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
    fun exportarCsv_shouldContainCorrectColumnHeaders() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("ID,Descrição,Valor,Tipo,Data,Categoria,Conta,Saldo da Conta")
    }

    @Test
    fun exportarCsv_shouldFormatValorWithTwoDecimalPlaces() {
        val usuario = usuarioMock()
        val t = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 5000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("5000.00")
        assertThat(content).doesNotContain("5000.0,")
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

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("Nubank,1975.00")
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
        assertThat(content).contains("\"1000.00\"")
        assertThat(content).contains("\"25.00\"")
        assertThat(content).contains("\"975.00\"")
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
    fun exportarCsv_shouldEscapeCommasInDescricao() {
        val usuario = usuarioMock()
        val t = Transacao().apply {
            id = 3L; descricao = "Salário, bônus"; valor = 100.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 8, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(exportAll(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("\"Salário, bônus\"")
    }
}
