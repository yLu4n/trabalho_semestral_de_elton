package br.com.finai.repository;
import br.com.finai.model.Lancamento;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    List<Lancamento> findByUsuarioIdAndExcluidoEmIsNullOrderByDataDescCriadoEmDesc(UUID usuarioId);
    List<Lancamento> findByUsuarioIdAndExcluidoEmIsNotNull(UUID usuarioId);
    Optional<Lancamento> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    boolean existsByCategoriaIdAndExcluidoEmIsNull(UUID categoriaId);
    List<Lancamento> findByExcluidoEmBefore(java.time.Instant limite);
}
