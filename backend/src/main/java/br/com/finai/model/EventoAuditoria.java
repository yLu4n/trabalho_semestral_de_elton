package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="evento_auditoria")
public class EventoAuditoria {
    @Id public UUID id = UUID.randomUUID();
    public String acao;
    public Instant criadoEm = Instant.now();
}
