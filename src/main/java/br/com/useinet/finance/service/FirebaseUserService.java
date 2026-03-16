package br.com.useinet.finance.service;

import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.repository.UsuarioRepository;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirebaseUserService {

    private final UsuarioRepository usuarioRepository;

    public FirebaseUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario findOrCreate(FirebaseToken token) {
        String uid = token.getUid();
        String email = token.getEmail();
        String name = token.getName();

        return usuarioRepository.findByProviderAndProviderId("firebase", uid)
                .orElseGet(() -> usuarioRepository.findByEmail(email)
                        .map(existing -> {
                            existing.setProvider("firebase");
                            existing.setProviderId(uid);
                            return usuarioRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            Usuario novo = new Usuario();
                            novo.setNome(name != null ? name : email);
                            novo.setEmail(email);
                            novo.setProvider("firebase");
                            novo.setProviderId(uid);
                            return usuarioRepository.save(novo);
                        }));
    }
}
