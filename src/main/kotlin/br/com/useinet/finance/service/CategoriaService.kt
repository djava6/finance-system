package br.com.useinet.finance.service

import br.com.useinet.finance.dto.CategoriaRequest
import br.com.useinet.finance.dto.CategoriaResponse
import br.com.useinet.finance.model.Categoria
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service

@Service
class CategoriaService(
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository
) {

    fun listar(): List<CategoriaResponse> =
        categoriaRepository.findAll().map { CategoriaResponse.from(it) }

    fun criar(request: CategoriaRequest): CategoriaResponse {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da categoria é obrigatório.")
        if (categoriaRepository.findByNome(request.nome).isPresent) throw IllegalArgumentException("Categoria já existe.")
        val categoria = Categoria().apply { this.nome = request.nome.trim() }
        return CategoriaResponse.from(categoriaRepository.save(categoria))
    }

    fun renomear(id: Long, request: CategoriaRequest): CategoriaResponse {
        if (request.nome.isNullOrBlank()) throw IllegalArgumentException("Nome da categoria é obrigatório.")
        val categoria = categoriaRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        if (categoriaRepository.findByNome(request.nome).isPresent) throw IllegalArgumentException("Já existe uma categoria com esse nome.")
        categoria.nome = request.nome.trim()
        return CategoriaResponse.from(categoriaRepository.save(categoria))
    }

    fun deletar(id: Long) {
        val categoria = categoriaRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
        if (transacaoRepository.existsByCategoria(categoria)) {
            throw IllegalArgumentException("Categoria em uso por transações e não pode ser excluída.")
        }
        categoriaRepository.delete(categoria)
    }
}
