package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Orcamento
import br.com.useinet.finance.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface OrcamentoRepository : JpaRepository<Orcamento, Long> {

    fun findByUsuario(usuario: Usuario): List<Orcamento>

    fun findByIdAndUsuario(id: Long, usuario: Usuario): Optional<Orcamento>

    @Query("""
        SELECT o FROM Orcamento o
        WHERE o.usuario = :usuario AND o.mes = :mes AND o.ano = :ano
    """)
    fun findByUsuarioAndMesAndAno(usuario: Usuario, mes: Int, ano: Int): List<Orcamento>
}
