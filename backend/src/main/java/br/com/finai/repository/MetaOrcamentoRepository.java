package br.com.finai.repository;
import br.com.finai.model.MetaOrcamento;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface MetaOrcamentoRepository extends JpaRepository<MetaOrcamento, UUID> {
    List<MetaOrcamento> findByUsuarioIdAndMes(UUID usuarioId, String mes);
    Optional<MetaOrcamento> findByUsuarioIdAndCategoriaIdAndMes(UUID usuarioId, UUID categoriaId, String mes);
    Optional<MetaOrcamento> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
