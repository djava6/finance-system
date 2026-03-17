package br.com.useinet.finance.service

import br.com.useinet.finance.dto.ContaResponse
import br.com.useinet.finance.dto.DashboardResponse
import br.com.useinet.finance.dto.DespesaPorCategoriaResponse
import br.com.useinet.finance.dto.EvolucaoMensalResponse
import br.com.useinet.finance.dto.TransacaoResponse
import br.com.useinet.finance.model.TipoTransacao
import br.com.useinet.finance.model.Usuario
import br.com.useinet.finance.repository.ContaRepository
import br.com.useinet.finance.repository.TransacaoRepository
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val transacaoRepository: TransacaoRepository,
    private val contaRepository: ContaRepository
) {

    fun getDashboard(usuario: Usuario): DashboardResponse {
        val totalReceitas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA)
        val totalDespesas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA)

        val contas = contaRepository.findByUsuario(usuario).map { ContaResponse.from(it) }
        val ultimasTransacoes = transacaoRepository.findTop10ByUsuarioOrderByDataDesc(usuario).map { TransacaoResponse.from(it) }
        val despesasPorCategoria = transacaoRepository
            .findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)
            .map { it as Array<*> }
            .map { row -> DespesaPorCategoriaResponse(row[0] as String, (row[1] as Number).toDouble()) }
        val evolucaoMensal = buildEvolucaoMensal(usuario)

        return DashboardResponse(totalReceitas, totalDespesas, contas, ultimasTransacoes, despesasPorCategoria, evolucaoMensal)
    }

    private fun buildEvolucaoMensal(usuario: Usuario): List<EvolucaoMensalResponse> {
        val map = LinkedHashMap<String, DoubleArray>()

        for (rawRow in transacaoRepository.findEvolucaoMensal(usuario)) {
            val row = rawRow as Array<*>
            val mes = (row[0] as Number).toInt()
            val ano = (row[1] as Number).toInt()
            val tipo = row[2].toString()
            val valor = (row[3] as Number).toDouble()

            val key = "$ano-$mes"
            val entry = map.getOrPut(key) { doubleArrayOf(ano.toDouble(), mes.toDouble(), 0.0, 0.0) }
            if (tipo == "RECEITA") entry[2] += valor else entry[3] += valor
        }

        return map.values.map { v -> EvolucaoMensalResponse(v[0].toInt(), v[1].toInt(), v[2], v[3]) }
    }
}
