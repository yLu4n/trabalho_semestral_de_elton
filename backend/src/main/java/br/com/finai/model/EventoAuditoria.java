package br.com.finai.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoria {
    @Id
    public UUID id = UUID.randomUUID();
    public String acao;
    public UUID correlacaoId;

    public static EventoAuditoria de(String acao, UUID correlacaoId) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.acao = acao;
        evento.correlacaoId = correlacaoId;
        return evento;
    }
    public Instant criadoEm = Instant.now();
}
