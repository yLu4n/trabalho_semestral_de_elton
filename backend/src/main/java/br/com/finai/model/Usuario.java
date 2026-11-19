package br.com.finai.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="usuario")
public class Usuario {
    @Id public UUID id = UUID.randomUUID();
    public String nome;
    @Column(unique=true) public String email;
    public String senhaHash;
    public LocalDate nascimento;
    public boolean emailVerificado;
    @Enumerated(EnumType.STRING) public Papel papel = Papel.TITULAR;
    public int tentativas;
    public Instant bloqueadoAte;
    public boolean bloqueado;
    public Instant exclusaoSolicitadaEm;
    public Instant criadoEm = Instant.now();
    @Version public long versao;
}
