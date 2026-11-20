package br.com.finai.service;

import br.com.finai.model.Categoria;
import br.com.finai.model.TipoLancamento;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CategoriasPorTextoTest {
    private final List<Categoria> categorias = List.of(
            categoria("Alimentação", false), categoria("Moradia", false),
            categoria("Transporte", false), categoria("Saúde", false),
            categoria("Educação", false), categoria("Lazer", false),
            categoria("Outras despesas", false), categoria("Salário", true),
            categoria("Outras receitas", true));

    @Test
    void reconheceTodasAsCategoriasPadrao() {
        Map.ofEntries(
                Map.entry("Paguei no supermercado", "Alimentação"),
                Map.entry("Pedido de Uber Eats", "Alimentação"),
                Map.entry("Paguei o condomínio", "Moradia"),
                Map.entry("Gastei com ônibus", "Transporte"),
                Map.entry("Fui ao hospital", "Saúde"),
                Map.entry("Paguei a mensalidade da faculdade", "Educação"),
                Map.entry("Comprei ingresso para o teatro", "Lazer"),
                Map.entry("Paguei uma tarifa bancária", "Outras despesas"),
                Map.entry("Recebi meu décimo terceiro", "Salário"),
                Map.entry("Recebi por um freela", "Outras receitas"),
                Map.entry("Recebi aluguel", "Outras receitas")
        ).forEach((frase, esperado) -> assertThat(CategoriasPorTexto.identificar(frase, categorias))
                .as(frase).isNotNull().extracting(c -> c.nome).isEqualTo(esperado));
    }

    @Test
    void naoForcaAssociacaoAmbiguaOuInativa() {
        assertThat(CategoriasPorTexto.identificar("Uber para o hospital", categorias)).isNull();
        assertThat(CategoriasPorTexto.identificar("Paguei e recebi", categorias)).isNull();
        Categoria pet = categoria("Cuidados com Pet", false);
        pet.usuarioId = java.util.UUID.randomUUID();
        assertThat(CategoriasPorTexto.identificar("Paguei R$ 80 em cuidados com PET", List.of(pet))).isSameAs(pet);
        pet.ativa = false;
        assertThat(CategoriasPorTexto.identificar("Cuidados com Pet", List.of(pet))).isNull();
    }

    private static Categoria categoria(String nome, boolean receita) {
        Categoria categoria = new Categoria();
        categoria.nome = nome;
        categoria.tipo = receita ? TipoLancamento.RECEITA : TipoLancamento.DESPESA;
        return categoria;
    }
}
