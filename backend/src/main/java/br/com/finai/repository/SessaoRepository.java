package br.com.finai.repository;
import br.com.finai.model.Sessao;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface SessaoRepository extends JpaRepository<Sessao, UUID> {
    void deleteByUsuarioId(UUID usuarioId);
    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update Sessao s set s.ultimaAtividade = :agora where s.id = :id and s.ultimaAtividade < :limite")
    int atualizarAtividade(UUID id, java.time.Instant agora, java.time.Instant limite);
}
