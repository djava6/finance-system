package br.com.useinet.finance.service;

import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvExportTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    private Usuario usuarioMock() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        return u;
    }

    @Test
    void exportarCsv_shouldReturnCsvWithBomAndHeader() {
        Usuario usuario = usuarioMock();
        when(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of());

        byte[] csv = transacaoService.exportarCsv(usuario);
        String content = new String(csv, StandardCharsets.UTF_8);

        // BOM present
        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);

        assertThat(content).contains("ID,Descrição,Valor,Tipo,Data,Categoria,Conta");
    }

    @Test
    void exportarCsv_shouldIncludeTransactionRows() {
        Usuario usuario = usuarioMock();

        Transacao t = new Transacao();
        t.setId(1L);
        t.setDescricao("Salário");
        t.setValor(5000.0);
        t.setTipo(TipoTransacao.RECEITA);
        t.setData(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of(t));

        byte[] csv = transacaoService.exportarCsv(usuario);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).contains("1,Salário,5000.0,RECEITA,15/03/2026 10:00,");
    }

    @Test
    void exportarCsv_shouldIncludeCategoryName() {
        Usuario usuario = usuarioMock();

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Alimentação");

        Transacao t = new Transacao();
        t.setId(2L);
        t.setDescricao("Mercado");
        t.setValor(200.0);
        t.setTipo(TipoTransacao.DESPESA);
        t.setData(LocalDateTime.of(2026, 3, 15, 12, 0));
        t.setCategoria(categoria);

        when(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of(t));

        byte[] csv = transacaoService.exportarCsv(usuario);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).contains("Alimentação");
    }

    @Test
    void exportarCsv_shouldEscapeCommasInDescricao() {
        Usuario usuario = usuarioMock();

        Transacao t = new Transacao();
        t.setId(3L);
        t.setDescricao("Salário, bônus");
        t.setValor(100.0);
        t.setTipo(TipoTransacao.RECEITA);
        t.setData(LocalDateTime.of(2026, 3, 15, 8, 0));

        when(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of(t));

        byte[] csv = transacaoService.exportarCsv(usuario);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).contains("\"Salário, bônus\"");
    }
}
