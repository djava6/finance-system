package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ContaRequest
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.ContaRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ContaServiceTest {

    @Mock lateinit var contaRepository: ContaRepository
    @InjectMocks lateinit var contaService: ContaService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    @Test
    fun listar_shouldReturnContasDoUsuario() {
        val usuario = usuarioMock()
        val c = Conta().apply { id = 1L; nome = "Banco"; saldo = 1000.0; this.usuario = usuario }
        `when`(contaRepository.findByUsuario(usuario)).thenReturn(listOf(c))

        val result = contaService.listar(usuario)
        assertThat(result).hasSize(1)
        assertThat(result[0].nome).isEqualTo("Banco")
        assertThat(result[0].saldo).isEqualTo(1000.0)
    }

    @Test
    fun criar_shouldSaveAndReturnConta() {
        val usuario = usuarioMock()
        val saved = Conta().apply { id = 1L; nome = "Poupança"; saldo = 500.0; this.usuario = usuario }
        `when`(contaRepository.save(any())).thenReturn(saved)

        val response = contaService.criar(ContaRequest(nome = "Poupança", saldo = 500.0), usuario)
        assertThat(response.nome).isEqualTo("Poupança")
        assertThat(response.saldo).isEqualTo(500.0)
        verify(contaRepository).save(any(Conta::class.java))
    }

    @Test
    fun criar_shouldThrowWhenNomeIsBlank() {
        assertThatThrownBy { contaService.criar(ContaRequest(nome = ""), usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Nome da conta é obrigatório")
    }

    @Test
    fun atualizar_shouldUpdateAndReturnConta() {
        val usuario = usuarioMock()
        val existing = Conta().apply { id = 1L; nome = "Antigo"; saldo = 100.0 }
        `when`(contaRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing))
        `when`(contaRepository.save(any())).thenAnswer { it.getArgument(0) }

        val response = contaService.atualizar(1L, ContaRequest(nome = "Novo", saldo = 200.0), usuario)
        assertThat(response.nome).isEqualTo("Novo")
        assertThat(response.saldo).isEqualTo(200.0)
    }

    @Test
    fun atualizar_shouldThrowWhenContaNotFound() {
        val usuario = usuarioMock()
        `when`(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty())
        assertThatThrownBy { contaService.atualizar(99L, ContaRequest(nome = "X"), usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Conta não encontrada")
    }

    @Test
    fun deletar_shouldRemoveContaDoUsuario() {
        val usuario = usuarioMock()
        val conta = Conta().apply { nome = "Remover" }
        `when`(contaRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(conta))
        contaService.deletar(1L, usuario)
        verify(contaRepository).delete(conta)
    }

    @Test
    fun deletar_shouldThrowWhenContaNotFound() {
        val usuario = usuarioMock()
        `when`(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty())
        assertThatThrownBy { contaService.deletar(99L, usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Conta não encontrada")
    }

    @Test
    fun criar_shouldAllowZeroSaldo() {
        val usuario = usuarioMock()
        val saved = Conta().apply { id = 1L; nome = "Zerada"; saldo = 0.0; this.usuario = usuario }
        `when`(contaRepository.save(any())).thenReturn(saved)

        val response = contaService.criar(ContaRequest(nome = "Zerada", saldo = 0.0), usuario)
        assertThat(response.saldo).isEqualTo(0.0)
    }

    @Test
    fun atualizar_shouldAllowNegativeSaldo() {
        val usuario = usuarioMock()
        val existing = Conta().apply { id = 1L; nome = "Antiga"; saldo = 100.0 }
        `when`(contaRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing))
        `when`(contaRepository.save(any())).thenAnswer { it.getArgument(0) }

        val response = contaService.atualizar(1L, ContaRequest(nome = "Antiga", saldo = -50.0), usuario)
        assertThat(response.saldo).isEqualTo(-50.0)
    }
}
