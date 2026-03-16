package br.com.useinet.finance.repository;

import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    List<Conta> findByUsuario(Usuario usuario);
    Optional<Conta> findByIdAndUsuario(Long id, Usuario usuario);
}
