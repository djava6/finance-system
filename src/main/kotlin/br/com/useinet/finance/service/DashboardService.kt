package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.dto.DashboardResponse
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DashboardService(
    private val transacaoRepository: TransacaoRepository,
    private val contaRepository: ContaRepository
) {

    @Transactional(readOnly = true)
    fun getDashboard(usuario: Usuario): DashboardResponse {
        val totalReceitas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)
        val totalDespesas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)
        val contas = contaRepository.findByUsuario(usuario).map { ContaResponse.from(it) }
        val ultimasTransacoes = transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario).map { TransacaoResponse.from(it) }
        val despesasPorCategoria = transacaoRepository.findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)
        val evolucaoMensal = transacaoRepository.findEvolucaoMensal(usuario)
        return DashboardResponse(totalReceitas, totalDespesas, contas, ultimasTransacoes, despesasPorCategoria, evolucaoMensal)
    }
}
