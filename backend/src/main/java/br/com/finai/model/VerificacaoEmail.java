package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="verificacao_email")
public class VerificacaoEmail {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public String tokenHash;
    public Instant expiraEm = Instant.now().plusSeconds(86400);
}
