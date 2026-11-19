package br.com.finai.repository;
import br.com.finai.model.RegistroInferencia;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RegistroInferenciaRepository extends JpaRepository<RegistroInferencia, UUID> {
    
}
