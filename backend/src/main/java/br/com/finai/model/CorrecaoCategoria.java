package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="correcao_categoria")
public class CorrecaoCategoria {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public String termo;
    public UUID categoriaSugerida;
    public UUID categoriaConfirmada;
    public Instant criadaEm = Instant.now();
}
