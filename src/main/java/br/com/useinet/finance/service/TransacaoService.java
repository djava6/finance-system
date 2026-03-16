package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.TransacaoRequest;
import br.com.useinet.finance.dto.TransacaoResponse;
import br.com.useinet.finance.model.Categoria;
import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.TipoTransacao;
import br.com.useinet.finance.model.Transacao;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.CategoriaRepository;
import br.com.useinet.finance.repository.ContaRepository;
import br.com.useinet.finance.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ContaRepository contaRepository;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            CategoriaRepository categoriaRepository,
                            ContaRepository contaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.contaRepository = contaRepository;
    }

    @Transactional
    public TransacaoResponse criar(TransacaoRequest request, Usuario usuario) {
        validarRequest(request);
        Transacao transacao = new Transacao();
        transacao.setDescricao(request.getDescricao());
        transacao.setValor(request.getValor());
        transacao.setTipo(request.getTipo());
        transacao.setData(request.getData() != null ? request.getData() : LocalDateTime.now());
        transacao.setUsuario(usuario);

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
            transacao.setCategoria(categoria);
        }

        if (request.getContaId() != null) {
            Conta conta = contaRepository.findByIdAndUsuario(request.getContaId(), usuario)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
            transacao.setConta(conta);
            ajustarSaldo(conta, request.getTipo(), request.getValor());
            contaRepository.save(conta);
        }

        return TransacaoResponse.from(transacaoRepository.save(transacao));
    }

    public List<TransacaoResponse> listar(Usuario usuario, LocalDateTime inicio, LocalDateTime fim) {
        List<Transacao> transacoes = (inicio != null && fim != null)
                ? transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDesc(usuario, inicio, fim)
                : transacaoRepository.findByUsuarioOrderByDataDesc(usuario);
        return transacoes.stream().map(TransacaoResponse::from).toList();
    }

    @Transactional
    public TransacaoResponse atualizar(Long id, TransacaoRequest request, Usuario usuario) {
        validarRequest(request);
        Transacao transacao = transacaoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));

        // Reverter efeito na conta anterior
        if (transacao.getConta() != null) {
            reverterSaldo(transacao.getConta(), transacao.getTipo(), transacao.getValor());
            contaRepository.save(transacao.getConta());
        }

        transacao.setDescricao(request.getDescricao());
        transacao.setValor(request.getValor());
        transacao.setTipo(request.getTipo());
        if (request.getData() != null) transacao.setData(request.getData());

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
            transacao.setCategoria(categoria);
        } else {
            transacao.setCategoria(null);
        }

        // Aplicar efeito na nova conta
        if (request.getContaId() != null) {
            Conta conta = contaRepository.findByIdAndUsuario(request.getContaId(), usuario)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
            transacao.setConta(conta);
            ajustarSaldo(conta, request.getTipo(), request.getValor());
            contaRepository.save(conta);
        } else {
            transacao.setConta(null);
        }

        return TransacaoResponse.from(transacaoRepository.save(transacao));
    }

    @Transactional
    public void deletar(Long id, Usuario usuario) {
        Transacao transacao = transacaoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));
        if (transacao.getConta() != null) {
            reverterSaldo(transacao.getConta(), transacao.getTipo(), transacao.getValor());
            contaRepository.save(transacao.getConta());
        }
        transacaoRepository.delete(transacao);
    }

    private void validarRequest(TransacaoRequest request) {
        if (request.getDescricao() == null || request.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição da transação é obrigatória.");
        }
        if (request.getValor() == null || request.getValor() <= 0) {
            throw new IllegalArgumentException("Valor da transação deve ser maior que zero.");
        }
        if (request.getTipo() == null) {
            throw new IllegalArgumentException("Tipo da transação é obrigatório.");
        }
    }

    private void ajustarSaldo(Conta conta, TipoTransacao tipo, Double valor) {
        if (tipo == TipoTransacao.RECEITA) {
            conta.setSaldo(conta.getSaldo() + valor);
        } else {
            conta.setSaldo(conta.getSaldo() - valor);
        }
    }

    private void reverterSaldo(Conta conta, TipoTransacao tipo, Double valor) {
        if (tipo == TipoTransacao.RECEITA) {
            conta.setSaldo(conta.getSaldo() - valor);
        } else {
            conta.setSaldo(conta.getSaldo() + valor);
        }
    }

    public byte[] exportarCsv(Usuario usuario) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM para compatibilidade com Excel
        csv.append("ID,Descrição,Valor,Tipo,Data,Categoria,Conta\n");

        transacaoRepository.findByUsuarioOrderByDataDesc(usuario).forEach(t -> {
            csv.append(t.getId()).append(',')
               .append(escapeCsv(t.getDescricao())).append(',')
               .append(t.getValor()).append(',')
               .append(t.getTipo()).append(',')
               .append(t.getData().format(fmt)).append(',')
               .append(t.getCategoria() != null ? escapeCsv(t.getCategoria().getNome()) : "").append(',')
               .append(t.getConta() != null ? escapeCsv(t.getConta().getNome()) : "")
               .append('\n');
        });

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}