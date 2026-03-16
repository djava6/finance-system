package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.DashboardResponse;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.ContaRepository;
import br.com.useinet.finance.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private ContaRepository contaRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Usuario usuarioMock() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        return u;
    }

    @Test
    void getDashboard_shouldCalculateSaldoCorretamente() {
        Usuario usuario = usuarioMock();

        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(5000.0);
        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(1500.0);
        when(contaRepository.findByUsuario(usuario)).thenReturn(List.of());
        when(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of());
        when(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(usuario);

        assertThat(response.getTotalReceitas()).isEqualTo(5000.0);
        assertThat(response.getTotalDespesas()).isEqualTo(1500.0);
        assertThat(response.getSaldo()).isEqualTo(3500.0);
    }

    @Test
    void getDashboard_shouldReturnUltimasTransacoes() {
        Usuario usuario = usuarioMock();

        Transacao t = new Transacao();
        t.setDescricao("Salário");
        t.setValor(5000.0);
        t.setTipo(TipoTransacao.RECEITA);
        t.setData(LocalDateTime.now());

        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(5000.0);
        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(0.0);
        when(contaRepository.findByUsuario(usuario)).thenReturn(List.of());
        when(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of(t));
        when(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(usuario);

        assertThat(response.getUltimasTransacoes()).hasSize(1);
        assertThat(response.getUltimasTransacoes().get(0).getDescricao()).isEqualTo("Salário");
    }

    @Test
    void getDashboard_shouldReturnDespesasPorCategoria() {
        Usuario usuario = usuarioMock();

        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(0.0);
        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(800.0);
        when(contaRepository.findByUsuario(usuario)).thenReturn(List.of());
        when(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of());
        when(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(List.of(
                new Object[]{"Alimentação", 500.0},
                new Object[]{"Transporte", 300.0}
        ));

        DashboardResponse response = dashboardService.getDashboard(usuario);

        assertThat(response.getDespesasPorCategoria()).hasSize(2);
        assertThat(response.getDespesasPorCategoria().get(0).getCategoria()).isEqualTo("Alimentação");
        assertThat(response.getDespesasPorCategoria().get(0).getTotal()).isEqualTo(500.0);
    }

    @Test
    void getDashboard_shouldReturnEvolucaoMensal() {
        Usuario usuario = usuarioMock();

        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).thenReturn(3000.0);
        when(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)).thenReturn(1000.0);
        when(contaRepository.findByUsuario(usuario)).thenReturn(List.of());
        when(transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of());
        when(transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)).thenReturn(List.of());
        when(transacaoRepository.findEvolucaoMensal(usuario)).thenReturn(List.of(
                new Object[]{1, 2025, "RECEITA", 3000.0},
                new Object[]{1, 2025, "DESPESA", 1000.0}
        ));

        DashboardResponse response = dashboardService.getDashboard(usuario);

        assertThat(response.getEvolucaoMensal()).hasSize(1);
        assertThat(response.getEvolucaoMensal().get(0).getMes()).isEqualTo(1);
        assertThat(response.getEvolucaoMensal().get(0).getAno()).isEqualTo(2025);
        assertThat(response.getEvolucaoMensal().get(0).getTotalReceitas()).isEqualTo(3000.0);
        assertThat(response.getEvolucaoMensal().get(0).getTotalDespesas()).isEqualTo(1000.0);
    }
}