package br.com.finai.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="meta_orcamento",uniqueConstraints=@UniqueConstraint(columnNames={"usuario_id","categoria_id","mes"}))
public class MetaOrcamento {
    @Id public UUID id = UUID.randomUUID();
    public UUID usuarioId;
    public UUID categoriaId;
    public String mes;
    @Column(precision=14,scale=2) public BigDecimal limite;
}
