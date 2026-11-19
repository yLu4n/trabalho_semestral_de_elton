package br.com.finai.repository;

import br.com.finai.model.SolicitacaoPrivacidade;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface SolicitacaoPrivacidadeRepository extends JpaRepository<SolicitacaoPrivacidade, UUID> {
    List<SolicitacaoPrivacidade> findByUsuarioId(UUID usuarioId);
}
