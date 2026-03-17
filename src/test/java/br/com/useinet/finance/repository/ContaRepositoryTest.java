package br.com.useinet.finance.repository;

import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ContaRepositoryTest {

    @MockBean
    private FirebaseAuth firebaseAuth;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario1;
    private Usuario usuario2;

    @BeforeEach
    void setUp() {
        usuario1 = salvarUsuario("user1_" + System.nanoTime() + "@conta.com");
        usuario2 = salvarUsuario("user2_" + System.nanoTime() + "@conta.com");
    }

    private Usuario salvarUsuario(String email) {
        Usuario u = new Usuario();
        u.setNome("Test");
        u.setEmail(email);
        return usuarioRepository.save(u);
    }

    private Conta salvarConta(String nome, double saldo, Usuario dono) {
        Conta c = new Conta();
        c.setNome(nome);
        c.setSaldo(saldo);
        c.setUsuario(dono);
        return contaRepository.save(c);
    }

    @Test
    void findByUsuario_shouldReturnOnlyOwnerContas() {
        salvarConta("Nubank", 1000.0, usuario1);
        salvarConta("Inter", 500.0, usuario1);
        salvarConta("BB", 200.0, usuario2);

        List<Conta> result = contaRepository.findByUsuario(usuario1);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Conta::getNome).containsExactlyInAnyOrder("Nubank", "Inter");
    }

    @Test
    void findByUsuario_shouldReturnEmptyWhenNoContas() {
        List<Conta> result = contaRepository.findByUsuario(usuario1);

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndUsuario_shouldReturnContaWhenOwnerMatches() {
        Conta conta = salvarConta("Poupança", 3000.0, usuario1);

        Optional<Conta> result = contaRepository.findByIdAndUsuario(conta.getId(), usuario1);

        assertThat(result).isPresent();
        assertThat(result.get().getNome()).isEqualTo("Poupança");
        assertThat(result.get().getSaldo()).isEqualTo(3000.0);
    }

    @Test
    void findByIdAndUsuario_shouldReturnEmptyWhenOwnerDiffers() {
        Conta conta = salvarConta("Secreta", 9999.0, usuario1);

        Optional<Conta> result = contaRepository.findByIdAndUsuario(conta.getId(), usuario2);

        assertThat(result).isEmpty();
    }
}
