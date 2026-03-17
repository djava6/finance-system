package br.com.useinet.finance.repository;

import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransacaoRepositoryTest {

    @MockBean
    private FirebaseAuth firebaseAuth;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setNome("Repo Test");
        u.setEmail("repotest_" + System.nanoTime() + "@test.com");
        u.setSenha("hashedpwd");
        usuario = usuarioRepository.save(u);
    }

    private Transacao salvar(String descricao, double valor, TipoTransacao tipo) {
        return salvar(descricao, valor, tipo, LocalDateTime.now(), null);
    }

    private Transacao salvar(String descricao, double valor, TipoTransacao tipo, LocalDateTime data, Categoria categoria) {
        Transacao t = new Transacao();
        t.setDescricao(descricao);
        t.setValor(valor);
        t.setTipo(tipo);
        t.setData(data);
        t.setCategoria(categoria);
        t.setUsuario(usuario);
        return transacaoRepository.save(t);
    }

    @Test
    void sumValorByUsuarioAndTipo_shouldReturnSumOfReceitas() {
        salvar("Salário", 5000.0, TipoTransacao.RECEITA);
        salvar("Freelance", 1000.0, TipoTransacao.RECEITA);
        salvar("Aluguel", 800.0, TipoTransacao.DESPESA);

        Double total = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA);

        assertThat(total).isEqualTo(6000.0);
    }

    @Test
    void sumValorByUsuarioAndTipo_shouldReturnZeroWhenNoTransactions() {
        Double total = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA);

        assertThat(total).isEqualTo(0.0);
    }

    @Test
    void findTop10ByUsuarioOrderByDataDesc_shouldReturnAtMost10() {
        LocalDateTime base = LocalDateTime.now();
        for (int i = 0; i < 15; i++) {
            salvar("T" + i, 10.0, TipoTransacao.DESPESA, base.minusDays(i), null);
        }

        List<Transacao> result = transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario);

        assertThat(result).hasSize(10);
        assertThat(result.get(0).getData()).isAfterOrEqualTo(result.get(9).getData());
    }

    @Test
    void findByIdAndUsuario_shouldReturnEmptyForOtherUser() {
        Usuario outro = new Usuario();
        outro.setNome("Outro");
        outro.setEmail("outro_" + System.nanoTime() + "@test.com");
        outro.setSenha("pwd");
        outro = usuarioRepository.save(outro);

        Transacao t = salvar("Minha", 100.0, TipoTransacao.RECEITA);

        Optional<Transacao> result = transacaoRepository.findByIdAndUsuario(t.getId(), outro);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByCategoria_shouldReturnTrueWhenInUse() {
        Categoria cat = new Categoria();
        cat.setNome("Alimentação_" + System.nanoTime());
        cat = categoriaRepository.save(cat);
        salvar("Mercado", 200.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat);

        assertThat(transacaoRepository.existsByCategoria(cat)).isTrue();
    }

    @Test
    void existsByCategoria_shouldReturnFalseWhenNotInUse() {
        Categoria cat = new Categoria();
        cat.setNome("Lazer_" + System.nanoTime());
        cat = categoriaRepository.save(cat);

        assertThat(transacaoRepository.existsByCategoria(cat)).isFalse();
    }

    @Test
    void findDespesasPorCategoria_shouldGroupByCategoria() {
        Categoria cat1 = categoriaRepository.save(categoria("Mercado_" + System.nanoTime()));
        Categoria cat2 = categoriaRepository.save(categoria("Transporte_" + System.nanoTime()));

        salvar("Supermercado", 300.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat1);
        salvar("Padaria", 50.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat1);
        salvar("Ônibus", 80.0, TipoTransacao.DESPESA, LocalDateTime.now(), cat2);

        List<Object[]> result = transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA);

        assertThat(result).hasSize(2);
        Object[] catRow = result.stream()
                .filter(r -> cat1.getNome().equals(r[0]))
                .findFirst().orElseThrow();
        assertThat((Double) catRow[1]).isEqualTo(350.0);
    }

    @Test
    void findEvolucaoMensal_shouldGroupByMonthAndYear() {
        LocalDateTime jan = LocalDateTime.of(2025, 1, 15, 0, 0);
        LocalDateTime feb = LocalDateTime.of(2025, 2, 10, 0, 0);

        salvar("Jan Receita", 3000.0, TipoTransacao.RECEITA, jan, null);
        salvar("Jan Despesa", 1000.0, TipoTransacao.DESPESA, jan, null);
        salvar("Fev Receita", 2000.0, TipoTransacao.RECEITA, feb, null);

        List<Object[]> result = transacaoRepository.findEvolucaoMensal(usuario);

        assertThat(result).hasSize(3);
    }

    @Test
    void findByUsuarioAndDataBetween_shouldFilterByDateRange() {
        LocalDateTime base = LocalDateTime.of(2025, 6, 15, 12, 0);
        salvar("Dentro", 100.0, TipoTransacao.RECEITA, base, null);
        salvar("Fora", 200.0, TipoTransacao.RECEITA, base.minusMonths(2), null);

        List<Transacao> result = transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(
                usuario, base.minusDays(1), base.plusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescricao()).isEqualTo("Dentro");
    }

    private Categoria categoria(String nome) {
        Categoria c = new Categoria();
        c.setNome(nome);
        return c;
    }
}
