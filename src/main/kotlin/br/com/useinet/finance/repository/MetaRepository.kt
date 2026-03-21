package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Meta
import br.com.useinet.finance.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MetaRepository : JpaRepository<Meta, Long> {
    fun findByUsuario(usuario: Usuario): List<Meta>
    fun findByIdAndUsuario(id: Long, usuario: Usuario): Optional<Meta>
}
