package br.com.useinet.finance.repository;

import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByUsuarioOrderByDataDesc(Usuario usuario);

    List<Transacao> findTop10ByUsuarioOrderByDataDesc(Usuario usuario);

    Optional<Transacao> findByIdAndUsuario(Long id, Usuario usuario);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t WHERE t.usuario = :usuario AND t.tipo = :tipo")
    Double sumValorByUsuarioAndTipo(@Param("usuario") Usuario usuario, @Param("tipo") TipoTransacao tipo);

    boolean existsByCategoria(Categoria categoria);

    @Query("SELECT COALESCE(t.categoria.nome, 'Sem categoria'), SUM(t.valor) " +
           "FROM Transacao t " +
           "WHERE t.usuario = :usuario AND t.tipo = :tipo " +
           "GROUP BY t.categoria.nome")
    List<Object[]> findDespesasPorCategoria(@Param("usuario") Usuario usuario, @Param("tipo") TipoTransacao tipo);

    List<Transacao> findByUsuarioAndDataBetweenOrderByDataDesc(Usuario usuario, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT MONTH(t.data), YEAR(t.data), t.tipo, SUM(t.valor) " +
           "FROM Transacao t WHERE t.usuario = :usuario " +
           "GROUP BY YEAR(t.data), MONTH(t.data), t.tipo " +
           "ORDER BY YEAR(t.data), MONTH(t.data)")
    List<Object[]> findEvolucaoMensal(@Param("usuario") Usuario usuario);
}