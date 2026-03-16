package br.com.useinet.finance.repository;

import br.com.useinet.finance.model.RefreshToken;
import br.com.useinet.finance.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUsuario(Usuario usuario);

    @Transactional
    @Modifying
    void deleteByExpiryDateBefore(Instant date);
}
