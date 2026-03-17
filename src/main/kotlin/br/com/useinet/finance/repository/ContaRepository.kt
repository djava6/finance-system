package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Conta
import br.com.useinet.finance.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ContaRepository : JpaRepository<Conta, Long> {
    fun findByUsuario(usuario: Usuario): List<Conta>
    fun findByIdAndUsuario(id: Long, usuario: Usuario): Optional<Conta>
}
