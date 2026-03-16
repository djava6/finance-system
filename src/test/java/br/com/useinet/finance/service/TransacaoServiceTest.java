package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.TransacaoRequest;
import br.com.useinet.finance.dto.TransacaoResponse;
import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.CategoriaRepository;
import br.com.useinet.finance.repository.ContaRepository;
import br.com.useinet.finance.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ContaRepository contaRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    private Usuario usuarioMock() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        return u;
    }

    @Test
    void criar_shouldSaveTransacaoAndReturnResponse() {
        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Salário");
        request.setValor(5000.0);
        request.setTipo(TipoTransacao.RECEITA);
        request.setData(LocalDateTime.now());

        Transacao saved = new Transacao();
        saved.setDescricao("Salário");
        saved.setValor(5000.0);
        saved.setTipo(TipoTransacao.RECEITA);
        saved.setData(request.getData());

        when(transacaoRepository.save(any())).thenReturn(saved);

        TransacaoResponse response = transacaoService.criar(request, usuarioMock());

        assertThat(response.getDescricao()).isEqualTo("Salário");
        assertThat(response.getValor()).isEqualTo(5000.0);
        assertThat(response.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    void criar_shouldSetDataNowWhenNotProvided() {
        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Mercado");
        request.setValor(150.0);
        request.setTipo(TipoTransacao.DESPESA);

        Transacao saved = new Transacao();
        saved.setDescricao("Mercado");
        saved.setValor(150.0);
        saved.setTipo(TipoTransacao.DESPESA);
        saved.setData(LocalDateTime.now());

        when(transacaoRepository.save(any())).thenReturn(saved);

        TransacaoResponse response = transacaoService.criar(request, usuarioMock());

        assertThat(response.getData()).isNotNull();
    }

    @Test
    void criar_shouldAssociarCategoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Alimentação");

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Restaurante");
        request.setValor(80.0);
        request.setTipo(TipoTransacao.DESPESA);
        request.setCategoriaId(1L);

        Transacao saved = new Transacao();
        saved.setDescricao("Restaurante");
        saved.setValor(80.0);
        saved.setTipo(TipoTransacao.DESPESA);
        saved.setData(LocalDateTime.now());
        saved.setCategoria(categoria);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(transacaoRepository.save(any())).thenReturn(saved);

        TransacaoResponse response = transacaoService.criar(request, usuarioMock());

        assertThat(response.getCategoriaId()).isEqualTo(1L);
        assertThat(response.getCategoria()).isEqualTo("Alimentação");
    }

    @Test
    void criar_shouldAssociarContaEAjustarSaldo() {
        Usuario usuario = usuarioMock();

        Conta conta = new Conta();
        conta.setId(10L);
        conta.setNome("Nubank");
        conta.setSaldo(1000.0);

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Salário");
        request.setValor(500.0);
        request.setTipo(TipoTransacao.RECEITA);
        request.setContaId(10L);

        Transacao saved = new Transacao();
        saved.setDescricao("Salário");
        saved.setValor(500.0);
        saved.setTipo(TipoTransacao.RECEITA);
        saved.setData(LocalDateTime.now());
        saved.setConta(conta);

        when(contaRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.of(conta));
        when(transacaoRepository.save(any())).thenReturn(saved);

        TransacaoResponse response = transacaoService.criar(request, usuario);

        assertThat(response.getContaId()).isEqualTo(10L);
        assertThat(response.getConta()).isEqualTo("Nubank");
        assertThat(conta.getSaldo()).isEqualTo(1500.0);
        verify(contaRepository).save(conta);
    }

    @Test
    void criar_shouldReduzirSaldoParaDespesa() {
        Usuario usuario = usuarioMock();

        Conta conta = new Conta();
        conta.setId(10L);
        conta.setNome("Nubank");
        conta.setSaldo(1000.0);

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Mercado");
        request.setValor(200.0);
        request.setTipo(TipoTransacao.DESPESA);
        request.setContaId(10L);

        Transacao saved = new Transacao();
        saved.setDescricao("Mercado");
        saved.setValor(200.0);
        saved.setTipo(TipoTransacao.DESPESA);
        saved.setData(LocalDateTime.now());
        saved.setConta(conta);

        when(contaRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.of(conta));
        when(transacaoRepository.save(any())).thenReturn(saved);

        transacaoService.criar(request, usuario);

        assertThat(conta.getSaldo()).isEqualTo(800.0);
    }

    @Test
    void criar_shouldThrowWhenContaNaoEncontrada() {
        Usuario usuario = usuarioMock();

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Test");
        request.setValor(100.0);
        request.setTipo(TipoTransacao.DESPESA);
        request.setContaId(99L);

        when(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transacaoService.criar(request, usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conta não encontrada");
    }

    @Test
    void deletar_shouldReverterSaldoConta() {
        Usuario usuario = usuarioMock();

        Conta conta = new Conta();
        conta.setId(10L);
        conta.setSaldo(800.0);

        Transacao transacao = new Transacao();
        transacao.setDescricao("Mercado");
        transacao.setValor(200.0);
        transacao.setTipo(TipoTransacao.DESPESA);
        transacao.setConta(conta);

        when(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(transacao));

        transacaoService.deletar(1L, usuario);

        assertThat(conta.getSaldo()).isEqualTo(1000.0);
        verify(contaRepository).save(conta);
        verify(transacaoRepository).delete(transacao);
    }

    @Test
    void atualizar_shouldReverterContaAnteriorEAplicarNova() {
        Usuario usuario = usuarioMock();

        Conta contaAntiga = new Conta();
        contaAntiga.setId(1L);
        contaAntiga.setSaldo(800.0);

        Conta contaNova = new Conta();
        contaNova.setId(2L);
        contaNova.setNome("Inter");
        contaNova.setSaldo(500.0);

        Transacao existing = new Transacao();
        existing.setDescricao("Original");
        existing.setValor(200.0);
        existing.setTipo(TipoTransacao.DESPESA);
        existing.setData(LocalDateTime.now());
        existing.setConta(contaAntiga);

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Atualizado");
        request.setValor(300.0);
        request.setTipo(TipoTransacao.DESPESA);
        request.setContaId(2L);

        when(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing));
        when(contaRepository.findByIdAndUsuario(2L, usuario)).thenReturn(Optional.of(contaNova));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transacaoService.atualizar(1L, request, usuario);

        // saldo da conta antiga deve ser revertido (despesa de 200 desfeita → +200)
        assertThat(contaAntiga.getSaldo()).isEqualTo(1000.0);
        // saldo da conta nova deve ser ajustado (despesa de 300 → -300)
        assertThat(contaNova.getSaldo()).isEqualTo(200.0);
    }

    @Test
    void listar_shouldReturnTransacoesDoUsuario() {
        Usuario usuario = usuarioMock();

        Transacao t1 = new Transacao();
        t1.setDescricao("Salário");
        t1.setValor(5000.0);
        t1.setTipo(TipoTransacao.RECEITA);
        t1.setData(LocalDateTime.now());

        when(transacaoRepository.findByUsuarioOrderByDataDesc(usuario)).thenReturn(List.of(t1));

        List<TransacaoResponse> result = transacaoService.listar(usuario, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescricao()).isEqualTo("Salário");
    }

    @Test
    void deletar_shouldRemoveTransacaoDoUsuario() {
        Usuario usuario = usuarioMock();

        Transacao transacao = new Transacao();
        transacao.setDescricao("Mercado");

        when(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(transacao));

        transacaoService.deletar(1L, usuario);

        verify(transacaoRepository).delete(transacao);
    }

    @Test
    void deletar_shouldThrowWhenTransacaoNotFound() {
        Usuario usuario = usuarioMock();

        when(transacaoRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transacaoService.deletar(99L, usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transação não encontrada");
    }

    @Test
    void atualizar_shouldUpdateAndReturnResponse() {
        Usuario usuario = usuarioMock();

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Novo Salário");
        request.setValor(6000.0);
        request.setTipo(TipoTransacao.RECEITA);

        Transacao existing = new Transacao();
        existing.setDescricao("Salário");
        existing.setValor(5000.0);
        existing.setTipo(TipoTransacao.RECEITA);
        existing.setData(LocalDateTime.now());

        when(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransacaoResponse response = transacaoService.atualizar(1L, request, usuario);

        assertThat(response.getDescricao()).isEqualTo("Novo Salário");
        assertThat(response.getValor()).isEqualTo(6000.0);
    }

    @Test
    void atualizar_shouldThrowWhenNotFound() {
        Usuario usuario = usuarioMock();

        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("X");
        request.setValor(10.0);
        request.setTipo(TipoTransacao.DESPESA);

        when(transacaoRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transacaoService.atualizar(99L, request, usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transação não encontrada");
    }

    @Test
    void criar_shouldThrowWhenDescricaoIsBlank() {
        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("");
        request.setValor(100.0);
        request.setTipo(TipoTransacao.DESPESA);

        assertThatThrownBy(() -> transacaoService.criar(request, usuarioMock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Descrição da transação é obrigatória");
    }

    @Test
    void criar_shouldThrowWhenValorIsZeroOrNegative() {
        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Test");
        request.setValor(-1.0);
        request.setTipo(TipoTransacao.DESPESA);

        assertThatThrownBy(() -> transacaoService.criar(request, usuarioMock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor da transação deve ser maior que zero");
    }

    @Test
    void criar_shouldThrowWhenTipoIsNull() {
        TransacaoRequest request = new TransacaoRequest();
        request.setDescricao("Test");
        request.setValor(100.0);
        request.setTipo(null);

        assertThatThrownBy(() -> transacaoService.criar(request, usuarioMock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo da transação é obrigatório");
    }
}