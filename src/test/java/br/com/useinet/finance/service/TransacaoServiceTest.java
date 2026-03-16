package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.TransacaoRequest;
import br.com.useinet.finance.dto.TransacaoResponse;
import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.CategoriaRepository;
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

        assertThat(response.getCategoria()).isEqualTo("Alimentação");
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