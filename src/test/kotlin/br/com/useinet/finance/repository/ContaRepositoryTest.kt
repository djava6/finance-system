package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class ContaRepositoryTest : IntegrationTestBase() {

    @Autowired lateinit var contaRepository: ContaRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository

    private lateinit var usuario1: Usuario
    private lateinit var usuario2: Usuario

    @BeforeEach
    fun setUp() {
        usuario1 = salvarUsuario("user1_${System.nanoTime()}@conta.com")
        usuario2 = salvarUsuario("user2_${System.nanoTime()}@conta.com")
    }

    private fun salvarUsuario(email: String): Usuario {
        return usuarioRepository.save(Usuario().apply { nome = "Test"; this.email = email })
    }

    private fun salvarConta(nome: String, saldo: Double, dono: Usuario): Conta {
        return contaRepository.save(Conta().apply { this.nome = nome; this.saldo = saldo; usuario = dono })
    }

    @Test
    fun findByUsuario_shouldReturnOnlyOwnerContas() {
        salvarConta("Nubank", 1000.0, usuario1)
        salvarConta("Inter", 500.0, usuario1)
        salvarConta("BB", 200.0, usuario2)

        val result = contaRepository.findByUsuario(usuario1)
        assertThat(result).hasSize(2)
        assertThat(result).extracting<String> { it.nome!! }.containsExactlyInAnyOrder("Nubank", "Inter")
    }

    @Test
    fun findByUsuario_shouldReturnEmptyWhenNoContas() {
        assertThat(contaRepository.findByUsuario(usuario1)).isEmpty()
    }

    @Test
    fun findByIdAndUsuario_shouldReturnContaWhenOwnerMatches() {
        val conta = salvarConta("Poupança", 3000.0, usuario1)
        val result = contaRepository.findByIdAndUsuario(conta.id!!, usuario1)
        assertThat(result).isPresent
        assertThat(result.get().nome).isEqualTo("Poupança")
        assertThat(result.get().saldo).isEqualTo(3000.0)
    }

    @Test
    fun findByIdAndUsuario_shouldReturnEmptyWhenOwnerDiffers() {
        val conta = salvarConta("Secreta", 9999.0, usuario1)
        assertThat(contaRepository.findByIdAndUsuario(conta.id!!, usuario2)).isEmpty()
    }

    @Test
    fun save_shouldPersistSaldoCorrectly() {
        val conta = contaRepository.save(Conta().apply { nome = "Com Saldo"; saldo = 1234.56; usuario = usuario1 })
        val found = contaRepository.findByIdAndUsuario(conta.id!!, usuario1)
        assertThat(found).isPresent
        assertThat(found.get().saldo).isEqualTo(1234.56)
    }

    @Test
    fun delete_shouldRemoveConta() {
        val conta = salvarConta("Para Remover", 100.0, usuario1)
        contaRepository.delete(conta)
        assertThat(contaRepository.findByIdAndUsuario(conta.id!!, usuario1)).isEmpty()
    }
}
