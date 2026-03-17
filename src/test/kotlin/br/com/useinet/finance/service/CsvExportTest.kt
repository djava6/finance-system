package br.com.useinet.finance.service

import br.com.useinet.finance.model.Categoria
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
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CsvExportTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var categoriaRepository: CategoriaRepository
    @Mock lateinit var contaRepository: ContaRepository
    @InjectMocks lateinit var transacaoService: TransacaoService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    @Test
    fun exportarCsv_shouldReturnCsvWithBomAndHeader() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())

        val csv = transacaoService.exportarCsv(usuario)
        val content = String(csv, StandardCharsets.UTF_8)

        assertThat(csv[0]).isEqualTo(0xEF.toByte())
        assertThat(csv[1]).isEqualTo(0xBB.toByte())
        assertThat(csv[2]).isEqualTo(0xBF.toByte())
        assertThat(content).contains("ID,Descrição,Valor,Tipo,Data,Categoria,Conta")
    }

    @Test
    fun exportarCsv_shouldIncludeTransactionRows() {
        val usuario = usuarioMock()
        val t = Transacao().apply {
            id = 1L; descricao = "Salário"; valor = 5000.0
            tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2026, 3, 15, 10, 0)
        }
        `when`(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))

        val content = String(transacaoService.exportarCsv(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("1,Salário,5000.0,RECEITA,15/03/2026 10:00,")
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

        val content = String(transacaoService.exportarCsv(usuario), StandardCharsets.UTF_8)
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

        val content = String(transacaoService.exportarCsv(usuario), StandardCharsets.UTF_8)
        assertThat(content).contains("\"Salário, bônus\"")
    }
}
