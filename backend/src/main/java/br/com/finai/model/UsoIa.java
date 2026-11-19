package br.com.finai.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "uso_ia", uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "dia" }))
public class UsoIa {
    @Id
    public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public LocalDate dia;
    public int quantidade;
}
