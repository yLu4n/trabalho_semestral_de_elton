package br.com.finai.repository;
import br.com.finai.model.UsoIa;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface UsoIaRepository extends JpaRepository<UsoIa, UUID> {
    List<UsoIa> findByUsuarioIdAndDiaBetween(UUID usuarioId, java.time.LocalDate inicio, java.time.LocalDate fim);
    Optional<UsoIa> findByUsuarioIdAndDia(UUID usuarioId, java.time.LocalDate dia);
}
