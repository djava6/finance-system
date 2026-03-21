package br.com.useinet.finance.service

import br.com.useinet.finance.dto.TransacaoRequest
import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TransacaoServiceTest {

    @Mock lateinit var transacaoRepository: TransacaoRepository
    @Mock lateinit var categoriaRepository: CategoriaRepository
    @Mock lateinit var contaRepository: ContaRepository
    @InjectMocks lateinit var transacaoService: TransacaoService

    private fun usuarioMock() = Usuario().apply { nome = "Carlos"; email = "carlos@email.com" }

    @Test
    fun criar_shouldSaveTransacaoAndReturnResponse() {
        val request = TransacaoRequest(descricao = "Salário", valor = 5000.0, tipo = TipoTransacao.RECEITA, data = LocalDateTime.now())
        val saved = Transacao().apply { descricao = "Salário"; valor = 5000.0; tipo = TipoTransacao.RECEITA; data = request.data }
        `when`(transacaoRepository.save(any())).thenReturn(saved)

        val response = transacaoService.criar(request, usuarioMock())
        assertThat(response.descricao).isEqualTo("Salário")
        assertThat(response.valor).isEqualTo(5000.0)
        assertThat(response.tipo).isEqualTo(TipoTransacao.RECEITA)
        verify(transacaoRepository).save(any(Transacao::class.java))
    }

    @Test
    fun criar_shouldSetDataNowWhenNotProvided() {
        val request = TransacaoRequest(descricao = "Mercado", valor = 150.0, tipo = TipoTransacao.DESPESA)
        val captured = mutableListOf<Transacao>()
        `when`(transacaoRepository.save(any())).thenAnswer { inv ->
            val t = inv.getArgument<Transacao>(0)
            captured.add(t)
            t
        }

        transacaoService.criar(request, usuarioMock())
        assertThat(captured[0].data).isNotNull
        assertThat(captured[0].data!!).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS))
    }

    @Test
    fun criar_shouldAssociarCategoria() {
        val categoria = Categoria().apply { id = 1L; nome = "Alimentação" }
        val request = TransacaoRequest(descricao = "Restaurante", valor = 80.0, tipo = TipoTransacao.DESPESA, categoriaId = 1L)
        val saved = Transacao().apply { descricao = "Restaurante"; valor = 80.0; tipo = TipoTransacao.DESPESA; data = LocalDateTime.now(); this.categoria = categoria }
        `when`(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria))
        `when`(transacaoRepository.save(any())).thenReturn(saved)

        val response = transacaoService.criar(request, usuarioMock())
        assertThat(response.categoriaId).isEqualTo(1L)
        assertThat(response.categoria).isEqualTo("Alimentação")
    }

    @Test
    fun criar_shouldAssociarContaEAjustarSaldo() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 10L; nome = "Nubank"; saldo = 1000.0 }
        val request = TransacaoRequest(descricao = "Salário", valor = 500.0, tipo = TipoTransacao.RECEITA, contaId = 10L)
        val saved = Transacao().apply { descricao = "Salário"; valor = 500.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.now(); this.conta = conta }
        `when`(contaRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.of(conta))
        `when`(transacaoRepository.save(any())).thenReturn(saved)

        val response = transacaoService.criar(request, usuario)
        assertThat(response.contaId).isEqualTo(10L)
        assertThat(response.conta).isEqualTo("Nubank")
        assertThat(conta.saldo).isEqualTo(1500.0)
        verify(contaRepository).save(conta)
    }

    @Test
    fun criar_shouldReduzirSaldoParaDespesa() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 10L; nome = "Nubank"; saldo = 1000.0 }
        val request = TransacaoRequest(descricao = "Mercado", valor = 200.0, tipo = TipoTransacao.DESPESA, contaId = 10L)
        val saved = Transacao().apply { descricao = "Mercado"; valor = 200.0; tipo = TipoTransacao.DESPESA; data = LocalDateTime.now(); this.conta = conta }
        `when`(contaRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.of(conta))
        `when`(transacaoRepository.save(any())).thenReturn(saved)

        transacaoService.criar(request, usuario)
        assertThat(conta.saldo).isEqualTo(800.0)
    }

    @Test
    fun criar_shouldThrowWhenContaNaoEncontrada() {
        val usuario = usuarioMock()
        val request = TransacaoRequest(descricao = "Test", valor = 100.0, tipo = TipoTransacao.DESPESA, contaId = 99L)
        `when`(contaRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty())
        assertThatThrownBy { transacaoService.criar(request, usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Conta não encontrada")
    }

    @Test
    fun deletar_shouldReverterSaldoConta() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 10L; saldo = 800.0 }
        val transacao = Transacao().apply { descricao = "Mercado"; valor = 200.0; tipo = TipoTransacao.DESPESA; this.conta = conta }
        `when`(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(transacao))

        transacaoService.deletar(1L, usuario)
        assertThat(conta.saldo).isEqualTo(1000.0)
        verify(contaRepository).save(conta)
        verify(transacaoRepository).delete(transacao)
    }

    @Test
    fun atualizar_shouldReverterContaAnteriorEAplicarNova() {
        val usuario = usuarioMock()
        val contaAntiga = Conta().apply { id = 1L; saldo = 800.0 }
        val contaNova = Conta().apply { id = 2L; nome = "Inter"; saldo = 500.0 }
        val existing = Transacao().apply { descricao = "Original"; valor = 200.0; tipo = TipoTransacao.DESPESA; data = LocalDateTime.now(); conta = contaAntiga }
        val request = TransacaoRequest(descricao = "Atualizado", valor = 300.0, tipo = TipoTransacao.DESPESA, contaId = 2L)

        whenever(transacaoRepository.findByIdAndUsuario(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(Optional.of(existing))
        whenever(contaRepository.findByIdAndUsuario(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(Optional.of(contaNova))
        whenever(contaRepository.save(org.mockito.kotlin.any())).thenReturn(contaAntiga)
        whenever(transacaoRepository.save(org.mockito.kotlin.any())).thenReturn(existing)

        transacaoService.atualizar(1L, request, usuario)
        assertThat(contaAntiga.saldo).isEqualTo(1000.0)
        assertThat(contaNova.saldo).isEqualTo(200.0)
    }

    @Test
    fun listar_shouldReturnTransacoesDoUsuario() {
        val usuario = usuarioMock()
        val t = Transacao().apply { descricao = "Salário"; valor = 5000.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.now() }
        val pageable = PageRequest.of(0, 20, Sort.by("data").descending())
        `when`(transacaoRepository.findByUsuario(usuario, pageable)).thenReturn(PageImpl(listOf(t)))

        val result = transacaoService.listar(usuario, null, null, pageable)
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].descricao).isEqualTo("Salário")
    }

    @Test
    fun deletar_shouldRemoveTransacaoDoUsuario() {
        val usuario = usuarioMock()
        val transacao = Transacao().apply { descricao = "Mercado" }
        `when`(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(transacao))
        transacaoService.deletar(1L, usuario)
        verify(transacaoRepository).delete(transacao)
    }

    @Test
    fun deletar_shouldThrowWhenTransacaoNotFound() {
        val usuario = usuarioMock()
        `when`(transacaoRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty())
        assertThatThrownBy { transacaoService.deletar(99L, usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Transação não encontrada")
    }

    @Test
    fun atualizar_shouldUpdateAndReturnResponse() {
        val usuario = usuarioMock()
        val request = TransacaoRequest(descricao = "Novo Salário", valor = 6000.0, tipo = TipoTransacao.RECEITA)
        val existing = Transacao().apply { descricao = "Salário"; valor = 5000.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.now() }
        `when`(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(Optional.of(existing))
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val response = transacaoService.atualizar(1L, request, usuario)
        assertThat(response.descricao).isEqualTo("Novo Salário")
        assertThat(response.valor).isEqualTo(6000.0)
    }

    @Test
    fun atualizar_shouldThrowWhenNotFound() {
        val usuario = usuarioMock()
        val request = TransacaoRequest(descricao = "X", valor = 10.0, tipo = TipoTransacao.DESPESA)
        `when`(transacaoRepository.findByIdAndUsuario(99L, usuario)).thenReturn(Optional.empty())
        assertThatThrownBy { transacaoService.atualizar(99L, request, usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Transação não encontrada")
    }

    @Test
    fun criar_shouldThrowWhenDescricaoIsBlank() {
        assertThatThrownBy { transacaoService.criar(TransacaoRequest(descricao = "", valor = 100.0, tipo = TipoTransacao.DESPESA), usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Descrição da transação é obrigatória")
    }

    @Test
    fun criar_shouldThrowWhenValorIsNegative() {
        assertThatThrownBy { transacaoService.criar(TransacaoRequest(descricao = "Test", valor = -1.0, tipo = TipoTransacao.DESPESA), usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Valor da transação deve ser maior que zero")
    }

    @Test
    fun criar_shouldThrowWhenValorIsZero() {
        assertThatThrownBy { transacaoService.criar(TransacaoRequest(descricao = "Test", valor = 0.0, tipo = TipoTransacao.DESPESA), usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Valor da transação deve ser maior que zero")
    }

    @Test
    fun criar_shouldThrowWhenTipoIsNull() {
        assertThatThrownBy { transacaoService.criar(TransacaoRequest(descricao = "Test", valor = 100.0, tipo = null), usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Tipo da transação é obrigatório")
    }

    @Test
    fun criar_shouldThrowWhenCategoriaNotFound() {
        val request = TransacaoRequest(descricao = "Test", valor = 100.0, tipo = TipoTransacao.DESPESA, categoriaId = 99L)
        `when`(categoriaRepository.findById(99L)).thenReturn(java.util.Optional.empty())
        assertThatThrownBy { transacaoService.criar(request, usuarioMock()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Categoria não encontrada")
    }

    @Test
    fun atualizar_shouldThrowWhenCategoriaNotFound() {
        val usuario = usuarioMock()
        val existing = Transacao().apply { descricao = "X"; valor = 100.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.now() }
        val request = TransacaoRequest(descricao = "X", valor = 100.0, tipo = TipoTransacao.RECEITA, categoriaId = 99L)
        `when`(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(java.util.Optional.of(existing))
        `when`(categoriaRepository.findById(99L)).thenReturn(java.util.Optional.empty())
        assertThatThrownBy { transacaoService.atualizar(1L, request, usuario) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Categoria não encontrada")
    }

    @Test
    fun listar_shouldUseDateFilterWhenInicioAndFimProvided() {
        val usuario = usuarioMock()
        val inicio = LocalDateTime.of(2025, 1, 1, 0, 0)
        val fim = LocalDateTime.of(2025, 12, 31, 23, 59)
        val pageable = PageRequest.of(0, 20)
        val t = Transacao().apply { descricao = "Filtrado"; valor = 500.0; tipo = TipoTransacao.RECEITA; data = LocalDateTime.of(2025, 6, 1, 0, 0) }
        `when`(transacaoRepository.findByUsuarioAndDataBetween(usuario, inicio, fim, pageable)).thenReturn(PageImpl(listOf(t)))

        val result = transacaoService.listar(usuario, inicio, fim, pageable)
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].descricao).isEqualTo("Filtrado")
        verify(transacaoRepository).findByUsuarioAndDataBetween(usuario, inicio, fim, pageable)
    }

    @Test
    fun atualizar_shouldClearContaWhenContaIdIsNull() {
        val usuario = usuarioMock()
        val conta = Conta().apply { id = 1L; saldo = 800.0 }
        val existing = Transacao().apply { descricao = "Original"; valor = 200.0; tipo = TipoTransacao.DESPESA; data = LocalDateTime.now(); this.conta = conta }
        val request = TransacaoRequest(descricao = "Atualizado", valor = 200.0, tipo = TipoTransacao.DESPESA, contaId = null)

        `when`(transacaoRepository.findByIdAndUsuario(1L, usuario)).thenReturn(java.util.Optional.of(existing))
        `when`(contaRepository.save(any())).thenReturn(conta)
        `when`(transacaoRepository.save(any())).thenAnswer { it.getArgument(0) }

        val response = transacaoService.atualizar(1L, request, usuario)
        assertThat(response.contaId).isNull()
        // conta anterior deve ter saldo revertido
        assertThat(conta.saldo).isEqualTo(1000.0)
    }
}
