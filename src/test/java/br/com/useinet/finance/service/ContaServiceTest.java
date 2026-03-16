package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.ContaRequest;
import br.com.useinet.finance.dto.ContaResponse;
import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.ContaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @InjectMocks
    private ContaService contaService;

    private Usuario usuarioMock() {
        Usuario u = new Usuario();
        u.setNome("Carlos");
        u.setEmail("carlos@email.com");
        return u;
    }

    @Test
    void listar_shouldReturnContasDoUsuario() {
        Usuario usuario = usuarioMock();

        Conta c = new Conta();
        c.setNome("Banco");
        c.setSaldo(1000.0);
        c.setUsuario(usuario);

        when(contaRepository.findByUsuario(usuario)).thenReturn(List.of(c));

        List<ContaResponse> result = contaService.listar(usuario);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Banco");
        assertThat(result.get(0).getSaldo()).isEqualTo(1000.0);
    }

    @Test
    void criar_shouldSaveAndReturnConta() {
        Usuario usuario = usuarioMock();

        ContaRequest request = new ContaRequest();
        request.setNome("Poupança");
        request.setSaldo(500.0);

        Conta saved = new Conta();
        saved.setNome("Poupança");
        saved.setSaldo(500.0);
        saved.setUsuario(usuario);

        when(contaRepository.save(any())).thenReturn(saved);

        ContaResponse response = contaService.criar(request, usuario);

        assertThat(response.getNome()).isEqualTo("Poupança");
        assertThat(response.getSaldo()).isEqualTo(500.0);
        verify(contaRepository).save(any(Conta.class));
    }

    @Test
    void criar_shouldThrowWhenNomeIsBlank() {
        ContaRequest request = new ContaRequest();
        request.setNome("");

        assertThatThrownBy(() -> contaService.criar(request, usuarioMock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome da conta é obrigatório");
    }

    @Test
    void atualizar_shouldUpdateAndReturnConta() {
        Usuario usuario = usuarioMock();

        Conta existing = new Conta();
        existing.setNome("Antigo");
        existing.setSaldo(100.0);

        ContaRequest request = new ContaRequest();
        request.setNome("Novo");
        request.setSaldo(200.0);

        when(contaRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing));
        when(contaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContaResponse response = contaService.atualizar(1L, request, usuario);

        assertThat(response.getNome()).isEqualTo("Novo");
        assertThat(response.getSaldo()).isEqualTo(200.0);
    }

    @Test
    void atualizar_shouldThrowWhenContaNotFound() {
        Usuario usuario = usuarioMock();

        ContaRequest request = new ContaRequest();
        request.setNome("X");

        when(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaService.atualizar(99L, request, usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conta não encontrada");
    }

    @Test
    void deletar_shouldRemoveContaDoUsuario() {
        Usuario usuario = usuarioMock();

        Conta conta = new Conta();
        conta.setNome("Remover");

        when(contaRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(conta));

        contaService.deletar(1L, usuario);

        verify(contaRepository).delete(conta);
    }

    @Test
    void deletar_shouldThrowWhenContaNotFound() {
        Usuario usuario = usuarioMock();

        when(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaService.deletar(99L, usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conta não encontrada");
    }
}
