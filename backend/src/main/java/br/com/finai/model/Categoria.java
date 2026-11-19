package br.com.finai.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public String nome;
    @Enumerated(EnumType.STRING)
    public TipoLancamento tipo;
    public String cor;
    public boolean ativa = true;
}
