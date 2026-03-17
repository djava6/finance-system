package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Categoria
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoriaRepository : JpaRepository<Categoria, Long> {
    fun findByNome(nome: String): Optional<Categoria>
}
