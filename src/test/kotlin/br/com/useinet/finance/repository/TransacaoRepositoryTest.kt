package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class TransacaoRepositoryTest {

    @MockBean lateinit var firebaseAuth: FirebaseAuth
    @Autowired lateinit var transacaoRepository: TransacaoRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var categoriaRepository: CategoriaRepository

    private lateinit var usuario: Usuario

    @BeforeEach
    fun setUp() {
        usuario = usuarioRepository.save(Usuario().apply {
            nome = "Repo Test"
            email = "repotest_${System.nanoTime()}@test.com"
        })
    }

    private fun salvar(descricao: String, valor: Double, tipo: TipoTransacao): Transacao =
        salvar(descricao, valor, tipo, LocalDateTime.now(), null)

    private fun salvar(descricao: String, valor: Double, tipo: TipoTransacao, data: LocalDateTime, categoria: Categoria?): Transacao {
        val outerUsuario = usuario
        return transacaoRepository.save(Transacao().apply {
            this.descricao = descricao
            this.valor = valor
            this.tipo = tipo
            this.data = data
            this.categoria = categoria
            this.usuario = outerUsuario
        })
    }

    @Test
    fun sumValorByUsuarioAndTipo_shouldReturnSumOfReceitas() {
        salvar("Salário", 5000.0, TipoTransacao.RECEITA)
        salvar("Freelance", 1000.0, TipoTransacao.RECEITA)
        salvar("Aluguel", 800.0, TipoTransacao.DESPESA)
        assertThat(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).isEqualTo(6000.0)
    }

    @Test
    fun sumValorByUsuarioAndTipo_shouldReturnZeroWhenNoTransactions() {
        assertThat(transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)).isEqualTo(0.0)
    }

    @Test
    fun findTop10ByUsuarioOrderByDataDesc_shouldReturnAtMost10() {
        val base = LocalDateTime.now()
        for (i in 0 until 15) salvar("T$i", 10.0, TipoTransacao.DESPESA, base.minusDays(i.toLong()), null)
        val result = transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario)
        assertThat(result).hasSize(10)
        assertThat(result[0].data).isAfterOrEqualTo(result[9].data)
    }

    @Test
    fun findByIdAndUsuario_shouldReturnEmptyForOtherUser() {
        val outro = usuarioRepository.save(Usuario().apply {
            nome = "Outro"
            email = "outro_${System.nanoTime()}@test.com"
        })
        val t = salvar("Minha", 100.0, TipoTransacao.RECEITA)
        assertThat(transacaoRepository.findByIdAndUsuario(t.id!!, outro)).isEmpty()
    }

    @Test
    fun existsByCategoria_shouldReturnTrueWhenInUse() {
        val cat = categoriaRepository.save(Categoria().apply { nome = "Alimentação_${System.nanoTime()}" })
        salvar("Mercado", 200.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat)
        assertThat(transacaoRepository.existsByCategoria(cat)).isTrue()
    }

    @Test
    fun existsByCategoria_shouldReturnFalseWhenNotInUse() {
        val cat = categoriaRepository.save(Categoria().apply { nome = "Lazer_${System.nanoTime()}" })
        assertThat(transacaoRepository.existsByCategoria(cat)).isFalse()
    }

    @Test
    fun findDespesasPorCategoria_shouldGroupByCategoria() {
        val cat1 = categoriaRepository.save(Categoria().apply { nome = "Mercado_${System.nanoTime()}" })
        val cat2 = categoriaRepository.save(Categoria().apply { nome = "Transporte_${System.nanoTime()}" })

        salvar("Supermercado", 300.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat1)
        salvar("Padaria", 50.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat1)
        salvar("Ônibus", 80.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat2)

        val result = transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)
        assertThat(result).hasSize(2)
        val catRow = result.map { it as Array<*> }.first { it[0] == cat1.nome }
        assertThat((catRow[1] as Number).toDouble()).isEqualTo(350.0)
    }

    @Test
    fun findEvolucaoMensal_shouldGroupByMonthAndYear() {
        val jan = LocalDateTime.of(2025, 1, 15, 0, 0)
        val feb = LocalDateTime.of(2025, 2, 10, 0, 0)
        salvar("Jan Receita", 3000.0, TipoTransacao.RECEITA, jan, null)
        salvar("Jan Despesa", 1000.0, TipoTransacao.DESPESA, jan, null)
        salvar("Fev Receita", 2000.0, TipoTransacao.RECEITA, feb, null)
        assertThat(transacaoRepository.findEvolucaoMensal(usuario)).hasSize(3)
    }

    @Test
    fun findByUsuarioAndDataBetween_shouldFilterByDateRange() {
        val base = LocalDateTime.of(2025, 6, 15, 12, 0)
        salvar("Dentro", 100.0, TipoTransacao.RECEITA, base, null)
        salvar("Fora", 200.0, TipoTransacao.RECEITA, base.minusMonths(2), null)
        val result = transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(usuario, base.minusDays(1), base.plusDays(1))
        assertThat(result).hasSize(1)
        assertThat(result[0].descricao).isEqualTo("Dentro")
    }
}
