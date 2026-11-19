package br.com.finai.repository;
import br.com.finai.model.SugestaoLancamento;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface SugestaoLancamentoRepository extends JpaRepository<SugestaoLancamento, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SugestaoLancamento s where s.id = :id and s.usuarioId = :usuarioId")
    Optional<SugestaoLancamento> obterParaConfirmacao(UUID id, UUID usuarioId);
    void deleteByCriadaEmBefore(java.time.Instant limite);
}
