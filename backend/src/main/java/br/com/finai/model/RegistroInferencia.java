package br.com.finai.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="registro_inferencia")
public class RegistroInferencia {
    @Id public UUID id = UUID.randomUUID();
    public String modelo;
    public String operacao;
    public long latenciaMs;
    @Column(precision=18,scale=8) public BigDecimal custoEstimado = BigDecimal.ZERO;
    public String resultado;
    public Instant criadoEm = Instant.now();
}
