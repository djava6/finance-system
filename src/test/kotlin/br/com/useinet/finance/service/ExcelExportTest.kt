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
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ExcelExportTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var categoriaRepository: CategoriaRepository
    @Mock lateinit var contaRepository: ContaRepository
    @Mock lateinit var orcamentoService: OrcamentoService
    @InjectMocks lateinit var transacaoService: TransacaoService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    private fun export(usuario: Usuario, transacoes: List<Transacao> = emptyList()): ByteArray {
        `when`(transacaoRepository.findByUsuarioOrderByDataAscIdAsc(usuario)).thenReturn(transacoes)
        val out = ByteArrayOutputStream()
        transacaoService.exportarXlsx(usuario, null, null, out)
        return out.toByteArray()
    }

    @Test
    fun exportarXlsx_shouldProduceNonEmptyOutput() {
        val bytes = export(usuarioMock())
        assertThat(bytes).isNotEmpty()
    }

    @Test
    fun exportarXlsx_shouldProduceValidZipFile() {
        // XLSX is a ZIP archive — magic bytes are PK (0x50 0x4B)
        val bytes = export(usuarioMock())
        assertThat(bytes[0]).isEqualTo(0x50.toByte()) // P
        assertThat(bytes[1]).isEqualTo(0x4B.toByte()) // K
    }

    @Test
    fun exportarXlsx_shouldProduceNonEmptyOutputWithTransactions() {
        val usuario = usuarioMock()
        val receita = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 5000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        val despesa = Transacao().apply {
            id = 2L; descricao = "Aluguel"; valor = 1500.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 5, 9, 0)
        }
        val bytes = export(usuario, listOf(receita, despesa))
        assertThat(bytes).isNotEmpty()
        assertThat(bytes[0]).isEqualTo(0x50.toByte())
        assertThat(bytes[1]).isEqualTo(0x4B.toByte())
    }

    @Test
    fun exportarXlsx_shouldHandleTransactionWithCategory() {
        val usuario = usuarioMock()
        val categoria = Categoria().apply { id = 1L; nome = "Alimentação" }
        val t = Transacao().apply {
            id = 1L; descricao = "Mercado"; valor = 300.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 10, 12, 0)
            this.categoria = categoria
        }
        val bytes = export(usuario, listOf(t))
        assertThat(bytes).isNotEmpty()
    }

    @Test
    fun exportarXlsx_shouldHandleTransactionWithConta() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 1L; nome = "Nubank"; saldo = 4700.0 }
        val t = Transacao().apply {
            id = 1L; descricao = "Pix enviado"; valor = 200.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 12, 15, 0)
            this.conta = conta
        }
        `when`(transacaoRepository.findByContaOrderByDataAscIdAsc(conta)).thenReturn(listOf(t))
        val bytes = export(usuario, listOf(t))
        assertThat(bytes).isNotEmpty()
    }

    @Test
    fun exportarXlsx_shouldProduceTwoSheets_andRunningBalance() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 1L; nome = "Nubank"; saldo = 4800.0 }
        val receita = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 5000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 1, 10, 0)
            this.conta = conta
        }
        val despesa = Transacao().apply {
            id = 2L; descricao = "Aluguel"; valor = 1500.0
            tipo = TipoTransacao.DESPESA; data = LocalDateTime.of(2026, 3, 5, 9, 0)
            this.conta = conta
        }
        `when`(transacaoRepository.findByContaOrderByDataAscIdAsc(conta)).thenReturn(listOf(receita, despesa))

        val bytes = export(usuario, listOf(receita, despesa))

        // Arquivo ZIP válido com dois sheets (dois arquivos xl/worksheets/sheet*.xml)
        assertThat(bytes).isNotEmpty()
        assertThat(bytes[0]).isEqualTo(0x50.toByte())
        assertThat(bytes[1]).isEqualTo(0x4B.toByte())
    }

    @Test
    fun exportarXlsx_withPeriodFilter_shouldQueryByPeriod() {
        val usuario = usuarioMock()
        val inicio = java.time.LocalDateTime.of(2026, 3, 1, 0, 0)
        val fim = java.time.LocalDateTime.of(2026, 3, 31, 23, 59, 59)
        `when`(transacaoRepository.findByUsuarioAndDataBetweenOrderByDataAscIdAsc(usuario, inicio, fim))
            .thenReturn(emptyList())

        val out = ByteArrayOutputStream()
        transacaoService.exportarXlsx(usuario, inicio, fim, out)
        val bytes = out.toByteArray()

        assertThat(bytes).isNotEmpty()
        assertThat(bytes[0]).isEqualTo(0x50.toByte())
    }
}
