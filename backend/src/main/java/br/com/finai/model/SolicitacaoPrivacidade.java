package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="solicitacao_privacidade")
public class SolicitacaoPrivacidade {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public String tipo;
    public String estado = "ABERTA";
    public Instant criadaEm = Instant.now();
}
