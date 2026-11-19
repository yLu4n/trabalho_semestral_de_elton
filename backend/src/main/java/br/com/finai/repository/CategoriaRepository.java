package br.com.finai.repository;
import br.com.finai.model.Categoria;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    @Query("select c from Categoria c where c.usuarioId = :usuarioId or c.usuarioId is null order by c.nome")
    List<Categoria> disponiveis(UUID usuarioId);
}
