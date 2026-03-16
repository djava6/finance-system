package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.DashboardResponse;
import br.com.useinet.finance.dto.DespesaPorCategoriaResponse;
import br.com.useinet.finance.dto.EvolucaoMensalResponse;
import br.com.useinet.finance.dto.TransacaoResponse;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final TransacaoRepository transacaoRepository;

    public DashboardService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public DashboardResponse getDashboard(Usuario usuario) {
        Double totalReceitas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.RECEITA);
        Double totalDespesas = transacaoRepository.sumValorByUsuarioAndTipo(usuario, TipoTransacao.DESPESA);

        List<TransacaoResponse> ultimasTransacoes = transacaoRepository
                .findTop10ByUsuarioOrderByDataDesc(usuario)
                .stream()
                .map(TransacaoResponse::from)
                .toList();

        List<DespesaPorCategoriaResponse> despesasPorCategoria = transacaoRepository
                .findDespesasPorCategoria(usuario, TipoTransacao.DESPESA)
                .stream()
                .map(row -> new DespesaPorCategoriaResponse((String) row[0], (Double) row[1]))
                .toList();

        List<EvolucaoMensalResponse> evolucaoMensal = buildEvolucaoMensal(usuario);

        return new DashboardResponse(totalReceitas, totalDespesas, ultimasTransacoes,
                despesasPorCategoria, evolucaoMensal);
    }

    private List<EvolucaoMensalResponse> buildEvolucaoMensal(Usuario usuario) {
        // key = "ano-mes"
        Map<String, double[]> map = new LinkedHashMap<>();

        for (Object[] row : transacaoRepository.findEvolucaoMensal(usuario)) {
            int mes = ((Number) row[0]).intValue();
            int ano = ((Number) row[1]).intValue();
            String tipo = row[2].toString();
            double valor = ((Number) row[3]).doubleValue();

            String key = ano + "-" + mes;
            map.computeIfAbsent(key, k -> new double[]{ano, mes, 0, 0});

            double[] entry = map.get(key);
            if ("RECEITA".equals(tipo)) entry[2] += valor;
            else entry[3] += valor;
        }

        List<EvolucaoMensalResponse> result = new ArrayList<>();
        for (double[] v : map.values()) {
            result.add(new EvolucaoMensalResponse((int) v[0], (int) v[1], v[2], v[3]));
        }
        return result;
    }
}
