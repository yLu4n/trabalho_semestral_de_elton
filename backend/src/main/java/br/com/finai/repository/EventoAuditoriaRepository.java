package br.com.finai.repository;
import br.com.finai.model.EventoAuditoria;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {
    
}
