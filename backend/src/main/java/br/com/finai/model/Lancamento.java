package br.com.finai.model;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="lancamento")
public class Lancamento {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public UUID categoriaId;
    @Enumerated(EnumType.STRING) public TipoLancamento tipo;
    @Column(precision=14,scale=2) public BigDecimal valor;
    public LocalDate data;
    public String descricao;
    public String estabelecimento;
    public String formaPagamento;
    public String origem = "MANUAL";
    public Instant excluidoEm;
    public Instant criadoEm = Instant.now();
    @Version public long versao;
}
