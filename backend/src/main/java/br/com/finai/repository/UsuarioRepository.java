package br.com.finai.repository;
import br.com.finai.model.Usuario;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByExclusaoSolicitadaEmBefore(java.time.Instant limite);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearParaAtualizacao(UUID id);
}
