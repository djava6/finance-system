package br.com.useinet.finance.service;

import br.com.useinet.finance.dto.ContaRequest;
import br.com.useinet.finance.dto.ContaResponse;
import br.com.useinet.finance.model.Conta;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.ContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;

    public ContaService(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    public List<ContaResponse> listar(Usuario usuario) {
        return contaRepository.findByUsuario(usuario).stream()
                .map(ContaResponse::from).toList();
    }

    @Transactional
    public ContaResponse criar(ContaRequest request, Usuario usuario) {
        validate(request);
        Conta conta = new Conta();
        conta.setNome(request.getNome().trim());
        conta.setSaldo(request.getSaldo() != null ? request.getSaldo() : 0.0);
        conta.setUsuario(usuario);
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponse atualizar(Long id, ContaRequest request, Usuario usuario) {
        validate(request);
        Conta conta = contaRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        conta.setNome(request.getNome().trim());
        if (request.getSaldo() != null) conta.setSaldo(request.getSaldo());
        return ContaResponse.from(contaRepository.save(conta));
    }

    @Transactional
    public void deletar(Long id, Usuario usuario) {
        Conta conta = contaRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        contaRepository.delete(conta);
    }

    private void validate(ContaRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da conta é obrigatório.");
        }
    }
}
