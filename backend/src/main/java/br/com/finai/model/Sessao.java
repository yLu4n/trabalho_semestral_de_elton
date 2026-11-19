package br.com.finai.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao")
public class Sessao {
    @Id
    public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public Instant criadaEm = Instant.now();
    public Instant ultimaAtividade = Instant.now();
}
