package br.com.useinet.finance.service

import br.com.useinet.finance.dto.OrcamentoRequest
import br.com.useinet.finance.dto.OrcamentoResponse
import br.com.useinet.finance.model.Orcamento
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.CategoriaRepository
import br.com.useinet.finance.repository.OrcamentoRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OrcamentoService(
    private val orcamentoRepository: OrcamentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository,
    private val notificationService: NotificationService
) {

    @Transactional(readOnly = true)
    fun listar(usuario: Usuario): List<OrcamentoResponse> =
        orcamentoRepository.findByUsuario(usuario).map { toResponse(it, usuario) }

    @Transactional(readOnly = true)
    fun listarPorMes(usuario: Usuario, mes: Int, ano: Int): List<OrcamentoResponse> =
        orcamentoRepository.findByUsuarioAndMesAndAno(usuario, mes, ano).map { toResponse(it, usuario) }

    @Transactional
    fun criar(request: OrcamentoRequest, usuario: Usuario): OrcamentoResponse {
        val valorLimite = request.valorLimite ?: throw IllegalArgumentException("Valor limite é obrigatório.")
        if (valorLimite <= 0) throw IllegalArgumentException("Valor limite deve ser maior que zero.")
        val mes = request.mes ?: throw IllegalArgumentException("Mês é obrigatório.")
        val ano = request.ano ?: throw IllegalArgumentException("Ano é obrigatório.")
        if (mes < 1 || mes > 12) throw IllegalArgumentException("Mês inválido.")

        val orcamento = Orcamento().apply {
            this.usuario = usuario
            this.valorLimite = valorLimite
            this.mes = mes
            this.ano = ano
            if (request.categoriaId != null) {
                this.categoria = categoriaRepository.findById(request.categoriaId)
                    .orElseThrow { IllegalArgumentException("Categoria não encontrada.") }
            }
        }
        return toResponse(orcamentoRepository.save(orcamento), usuario)
    }

    @Transactional
    fun atualizar(id: Long, request: OrcamentoRequest, usuario: Usuario): OrcamentoResponse {
        val orcamento = orcamentoRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Orçamento não encontrado.") }
        val valorLimite = request.valorLimite ?: throw IllegalArgumentException("Valor limite é obrigatório.")
        if (valorLimite <= 0) throw IllegalArgumentException("Valor limite deve ser maior que zero.")
        orcamento.valorLimite = valorLimite
        return toResponse(orcamentoRepository.save(orcamento), usuario)
    }

    @Transactional
    fun deletar(id: Long, usuario: Usuario) {
        val orcamento = orcamentoRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow { IllegalArgumentException("Orçamento não encontrado.") }
        orcamentoRepository.delete(orcamento)
    }

    fun checkAlerts(usuario: Usuario, mes: Int, ano: Int) {
        val inicio = LocalDate.of(ano, mes, 1)
        val fim = inicio.plusMonths(1)
        orcamentoRepository.findByUsuarioAndMesAndAno(usuario, mes, ano).forEach { orc ->
            val gasto = calcularGasto(orc, usuario, inicio, fim)
            val pct = if (orc.valorLimite > 0) (gasto / orc.valorLimite) * 100.0 else 0.0
            val catName = orc.categoria?.nome ?: "Sem categoria"
            when {
                pct >= 100.0 -> notificationService.send(
                    usuario,
                    "Orçamento estourado!",
                    "Você ultrapassou 100% do orçamento de $catName."
                )
                pct >= 80.0 -> notificationService.send(
                    usuario,
                    "Orçamento quase no limite",
                    "Você atingiu ${pct.toInt()}% do orçamento de $catName."
                )
            }
        }
    }

    private fun toResponse(o: Orcamento, usuario: Usuario): OrcamentoResponse {
        val inicio = LocalDate.of(o.ano, o.mes, 1)
        val fim = inicio.plusMonths(1)
        val gasto = calcularGasto(o, usuario, inicio, fim)
        return OrcamentoResponse.from(o, gasto)
    }

    private fun calcularGasto(o: Orcamento, usuario: Usuario, inicio: LocalDate, fim: LocalDate): Double {
        return if (o.categoria != null) {
            transacaoRepository.sumDespesasByCategoriaAndPeriod(usuario, o.categoria!!, inicio, fim)
        } else {
            transacaoRepository.sumDespesasSemCategoriaAndPeriod(usuario, inicio, fim)
        }
    }
}
