package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="sugestao_lancamento")
public class SugestaoLancamento {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public String textoOriginal;
    @Column(columnDefinition="text") public String dados;
    public Instant criadaEm = Instant.now();
}
