package br.com.finai.repository;

import br.com.finai.model.VerificacaoEmail;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface VerificacaoEmailRepository extends JpaRepository<VerificacaoEmail, UUID> {
    Optional<VerificacaoEmail> findByTokenHash(String tokenHash);

    void deleteByUsuarioIdAndRecuperacaoTrue(UUID usuarioId);
}
