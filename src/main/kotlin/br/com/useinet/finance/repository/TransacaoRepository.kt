package br.com.useinet.finance.repository

import br.com.useinet.finance.dto.DespesaPorCategoriaResponse
import br.com.useinet.finance.dto.EvolucaoMensalResponse
import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Transacao
import br.com.useinet.finance.model.Usuario
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface TransacaoRepository : JpaRepository<Transacao, Long> {

    fun findByUsuarioOrderByDataDesc(usuario: Usuario): List<Transacao>

    fun findByUsuario(usuario: Usuario, pageable: Pageable): Page<Transacao>

    fun findByUsuarioAndDataBetween(
        usuario: Usuario,
        inicio: LocalDateTime,
        fim: LocalDateTime,
        pageable: Pageable
    ): Page<Transacao>

    fun existsByUsuarioAndDataAndValorAndDescricao(
        usuario: Usuario,
        data: LocalDateTime,
        valor: Double,
        descricao: String
    ): Boolean

    fun findTop10ByUsuarioOrderByDataDesc(usuario: Usuario): List<Transacao>

    fun findByIdAndUsuario(id: Long, usuario: Usuario): Optional<Transacao>

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t WHERE t.usuario = :usuario AND t.tipo = :tipo")
    fun sumValorByUsuarioAndTipo(@Param("usuario") usuario: Usuario, @Param("tipo") tipo: TipoTransacao): Double

    fun existsByCategoria(categoria: Categoria): Boolean

    @Query("""
        SELECT new br.com.useinet.finance.dto.DespesaPorCategoriaResponse(
            COALESCE(t.categoria.nome, 'Sem categoria'), SUM(t.valor)
        )
        FROM Transacao t
        WHERE t.usuario = :usuario AND t.tipo = :tipo
        GROUP BY COALESCE(t.categoria.nome, 'Sem categoria')
    """)
    fun findDespesasPorCategoria(
        @Param("usuario") usuario: Usuario,
        @Param("tipo") tipo: TipoTransacao
    ): List<DespesaPorCategoriaResponse>

    fun findByUsuarioAndDataBetweenOrderByDataDesc(
        usuario: Usuario,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Transacao>

    fun findByContaOrderByDataAsc(conta: Conta): List<Transacao>

    @Query("""
        SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
        WHERE t.usuario = :usuario
          AND t.tipo = br.com.useinet.finance.model.TipoTransacao.DESPESA
          AND t.categoria = :categoria
          AND t.data >= :inicio AND t.data < :fim
    """)
    fun sumDespesasByCategoriaAndPeriod(
        @Param("usuario") usuario: Usuario,
        @Param("categoria") categoria: Categoria,
        @Param("inicio") inicio: LocalDateTime,
        @Param("fim") fim: LocalDateTime
    ): Double

    @Query("""
        SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
        WHERE t.usuario = :usuario
          AND t.tipo = br.com.useinet.finance.model.TipoTransacao.DESPESA
          AND t.categoria IS NULL
          AND t.data >= :inicio AND t.data < :fim
    """)
    fun sumDespesasSemCategoriaAndPeriod(
        @Param("usuario") usuario: Usuario,
        @Param("inicio") inicio: LocalDateTime,
        @Param("fim") fim: LocalDateTime
    ): Double

    @Query("""
        SELECT new br.com.useinet.finance.dto.EvolucaoMensalResponse(
            YEAR(t.data), MONTH(t.data),
            SUM(CASE WHEN t.tipo = br.com.useinet.finance.model.TipoTransacao.RECEITA THEN t.valor ELSE 0.0 END),
            SUM(CASE WHEN t.tipo = br.com.useinet.finance.model.TipoTransacao.DESPESA THEN t.valor ELSE 0.0 END)
        )
        FROM Transacao t
        WHERE t.usuario = :usuario
        GROUP BY YEAR(t.data), MONTH(t.data)
        ORDER BY YEAR(t.data), MONTH(t.data)
    """)
    fun findEvolucaoMensal(@Param("usuario") usuario: Usuario): List<EvolucaoMensalResponse>
}
