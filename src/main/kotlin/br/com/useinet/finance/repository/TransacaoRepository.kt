package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface TransacaoRepository : JpaRepository<Transacao, Long> {

    fun findByUsuarioOrderByDataDesc(usuario: Usuario): List<Transacao>

    fun findTop10ByUsuarioOrderByDataDesc(usuario: Usuario): List<Transacao>

    fun findByIdAndUsuario(id: Long, usuario: Usuario): Optional<Transacao>

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t WHERE t.usuario = :usuario AND t.tipo = :tipo")
    fun sumValorByUsuarioAndTipo(@Param("usuario") usuario: Usuario, @Param("tipo") tipo: TipoTransacao): Double

    fun existsByCategoria(categoria: Categoria): Boolean

    @Query(
        "SELECT COALESCE(t.categoria.nome, 'Sem categoria'), SUM(t.valor) " +
        "FROM Transacao t " +
        "WHERE t.usuario = :usuario AND t.tipo = :tipo " +
        "GROUP BY t.categoria.nome"
    )
    fun findDespesasPorCategoria(@Param("usuario") usuario: Usuario, @Param("tipo") tipo: TipoTransacao): List<Any>

    fun findByUsuarioAndDataBetweenOrderByDataDesc(
        usuario: Usuario,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Transacao>

    @Query(
        "SELECT MONTH(t.data), YEAR(t.data), t.tipo, SUM(t.valor) " +
        "FROM Transacao t WHERE t.usuario = :usuario " +
        "GROUP BY YEAR(t.data), MONTH(t.data), t.tipo " +
        "ORDER BY YEAR(t.data), MONTH(t.data)"
    )
    fun findEvolucaoMensal(@Param("usuario") usuario: Usuario): List<Any>
}
