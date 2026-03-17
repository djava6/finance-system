package br.com.useinet.finance.service

import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DashboardServiceTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var contaRepository: ContaRepository
    @InjectMocks lateinit var dashboardService: DashboardService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    @Test
    fun getDashboard_shouldCalculateSaldoCorretamente() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(5000.0)
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(1500.0)
        `when`(contaRepository.findByUsuario(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(emptyList())

        val response = dashboardService.getDashboard(usuario)
        assertThat(response.totalReceitas).isEqualTo(5000.0)
        assertThat(response.totalDespesas).isEqualTo(1500.0)
        assertThat(response.saldo).isEqualTo(3500.0)
    }

    @Test
    fun getDashboard_shouldReturnUltimasTransacoes() {
        val usuario = usuarioMock()
        val t = Transacao().apply { descricao = "Salário"; valor = 5000.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.now() }
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(5000.0)
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(0.0)
        `when`(contaRepository.findByUsuario(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(listOf(t))
        `when`(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(emptyList())

        val response = dashboardService.getDashboard(usuario)
        assertThat(response.ultimasTransacoes).hasSize(1)
        assertThat(response.ultimasTransacoes[0].descricao).isEqualTo("Salário")
    }

    @Test
    fun getDashboard_shouldReturnDespesasPorCategoria() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(0.0)
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(800.0)
        `when`(contaRepository.findByUsuario(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(listOf(
            arrayOf("Alimentação", 500.0),
            arrayOf("Transporte", 300.0)
        ))

        val response = dashboardService.getDashboard(usuario)
        assertThat(response.despesasPorCategoria).hasSize(2)
        assertThat(response.despesasPorCategoria[0].categoria).isEqualTo("Alimentação")
        assertThat(response.despesasPorCategoria[0].total).isEqualTo(500.0)
    }

    @Test
    fun getDashboard_shouldReturnEvolucaoMensal() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(3000.0)
        `when`(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(1000.0)
        `when`(contaRepository.findByUsuario(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(emptyList())
        `when`(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(emptyList())
        `when`(transacaoRepository.findEvolucaoMensal(usuario)).thenReturn(listOf(
            arrayOf(1, 2025, "RECEITA", 3000.0),
            arrayOf(1, 2025, "DESPESA", 1000.0)
        ))

        val response = dashboardService.getDashboard(usuario)
        assertThat(response.evolucaoMensal).hasSize(1)
        assertThat(response.evolucaoMensal[0].mes).isEqualTo(1)
        assertThat(response.evolucaoMensal[0].ano).isEqualTo(2025)
        assertThat(response.evolucaoMensal[0].totalReceitas).isEqualTo(3000.0)
        assertThat(response.evolucaoMensal[0].totalDespesas).isEqualTo(1000.0)
    }
}
