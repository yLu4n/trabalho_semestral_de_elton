package br.com.finai.repository;

import br.com.finai.model.CorrecaoCategoria;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CorrecaoCategoriaRepository extends JpaRepository<CorrecaoCategoria, UUID> {
    List<CorrecaoCategoria> findTop100ByUsuarioIdOrderByCriadaEmDesc(UUID usuarioId);
}
