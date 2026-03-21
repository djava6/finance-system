package br.com.useinet.finance.repository

import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class CategoriaRepositoryTest : IntegrationTestBase() {

    @Autowired lateinit var categoriaRepository: CategoriaRepository

    // Prefix avoids collisions with default categories seeded by Flyway V5
    private val p = "REPO_TEST_"

    @Test
    fun findByNome_shouldReturnCategoriaWhenExists() {
        val saved = categoriaRepository.save(Categoria().apply { nome = "${p}Alimentação" })

        val result = categoriaRepository.findByNome("${p}Alimentação")

        assertThat(result).isPresent
        assertThat(result.get().id).isEqualTo(saved.id)
        assertThat(result.get().nome).isEqualTo("${p}Alimentação")
    }

    @Test
    fun findByNome_shouldReturnEmptyWhenNotFound() {
        val result = categoriaRepository.findByNome("${p}NaoExiste")
        assertThat(result).isEmpty
    }

    @Test
    fun save_shouldPersistAndGenerateId() {
        val cat = categoriaRepository.save(Categoria().apply { nome = "${p}Transporte" })
        assertThat(cat.id).isNotNull
        assertThat(cat.nome).isEqualTo("${p}Transporte")
    }

    @Test
    fun delete_shouldRemoveCategoria() {
        val cat = categoriaRepository.save(Categoria().apply { nome = "${p}Remover" })
        categoriaRepository.delete(cat)
        assertThat(categoriaRepository.findByNome("${p}Remover")).isEmpty
    }
}
