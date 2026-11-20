package br.com.finai.service;

import br.com.finai.model.Categoria;
import br.com.finai.model.TipoLancamento;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class CategoriasPorTexto {
    private static final Map<String, Set<String>> TERMOS = Map.of(
            "alimentacao", termos("comida|refeição|refeições|almoço|almocei|jantar|jantei|lanche|lanches|"
                    + "mercearia|mercado|supermercado|feira livre|hortifruti|sacolão|açougue|padaria|"
                    + "restaurante|restaurantes|cafeteria|café da manhã|pizza|pizzaria|hambúrguer|marmita|"
                    + "marmitas|sushi|açaí|sorvete|sorveteria|ifood|uber eats|delivery de comida"),
            "moradia", termos("aluguel|aluguéis|alugueis|condomínio|condomínios|prestação da casa|"
                    + "financiamento imobiliário|conta de luz|conta de água|conta de energia|energia elétrica|"
                    + "gás de cozinha|botijão|iptu|seguro residencial|internet residencial|reforma da casa|"
                    + "manutenção da casa|encanador|eletricista|diarista|faxina"),
            "transporte", termos("uber|onibus|transporte|táxi|táxis|metrô|brt|trem|passagem|passagens|"
                    + "bilhete único|vale transporte|corrida de aplicativo|combustível|gasolina|etanol|diesel|"
                    + "abasteci|abastecimento|pedágio|estacionamento|ipva|licenciamento do carro|"
                    + "oficina mecânica|manutenção do carro|seguro auto|seguro do carro|passagem aérea"),
            "saude", termos("hospital|hospitais|fui ao hospital|farmácia|farmácias|drogaria|remédio|remédios|"
                    + "medicamento|medicamentos|consulta|consulta médica|consultório|médico|médica|dentista|"
                    + "plano de saúde|convênio médico|exame|exames|laboratório|terapia|psicólogo|psicóloga|"
                    + "fisioterapia|vacina|vacinas|cirurgia|pronto socorro|clínica|tratamento odontológico"),
            "educacao", termos("escola|escolar|faculdade|universidade|curso|cursos|matrícula|rematrícula|"
                    + "mensalidade escolar|mensalidade da faculdade|material escolar|livro didático|apostila|"
                    + "apostilas|aula|aulas|professor particular|reforço escolar|pós graduação|"
                    + "graduação|curso técnico|curso de idiomas|taxa de inscrição no vestibular"),
            "lazer", termos("cinema|filme|filmes|teatro|show|shows|concerto|ingresso|ingressos|parque de diversões|"
                    + "museu|passeio|passeios|entretenimento|netflix|spotify|streaming|videogame|"
                    + "jogo eletrônico|jogos eletrônicos|playstation|xbox|festa|balada|karaokê"),
            "outras despesas", termos("gasto diverso|gastos diversos|despesa extra|despesas extras|"
                    + "taxa bancária|tarifa bancária|multa|multas|presente|presentes|roupa|roupas|vestuário|"
                    + "calçado|calçados|cabeleireiro|barbearia|manicure|cartório|doação feita|doei"),
            "salario", termos("salário|salários|pagamento da empresa|pagamento do trabalho|folha de pagamento|"
                    + "contracheque|holerite|ordenado|remuneração|décimo terceiro|13º salário|"
                    + "adiantamento salarial|vale salarial|bônus salarial"),
            "outras receitas", termos("renda extra|receita extra|receitas extras|freela|freelance|bico|"
                    + "reembolso|estorno|cashback|dividendo|dividendos|rendimento|rendimentos|juros recebidos|"
                    + "vendi|venda de usado|prêmio recebido|doação recebida|recebi aluguel|aluguel recebido|"
                    + "pensão recebida|aposentadoria|benefício recebido")
    );
    private static final Pattern RECEBIMENTO = Pattern.compile("\\b(recebi|receita|reembolso|estorno|ganhei|vendi)\\b");
    private static final Pattern PAGAMENTO = Pattern.compile("\\b(paguei|gastei|comprei|despesa|doei)\\b");
    private static final Pattern UBER_EATS = Pattern.compile("\\buber eats\\b");

    private CategoriasPorTexto() {}

    static Categoria identificar(String texto, List<Categoria> categorias) {
        String normalizado = RegrasFinanceiras.normalizar(texto);
        boolean recebeu = RECEBIMENTO.matcher(normalizado).find();
        boolean pagou = PAGAMENTO.matcher(normalizado).find();
        if (recebeu && pagou) return null;
        TipoLancamento tipo = recebeu ? TipoLancamento.RECEITA : pagou ? TipoLancamento.DESPESA : null;
        var disponiveis = categorias.stream().filter(c -> c.ativa && (tipo == null || c.tipo == tipo)).toList();
        var exatas = disponiveis.stream().filter(c -> normalizado.equals(RegrasFinanceiras.normalizar(c.nome))).toList();
        if (!exatas.isEmpty()) return unica(exatas);
        return unica(disponiveis.stream().filter(c -> menciona(normalizado, c)).toList());
    }

    static boolean apenasCategoria(String texto, Categoria categoria) {
        String normalizado = RegrasFinanceiras.normalizar(texto);
        String nome = RegrasFinanceiras.normalizar(categoria.nome);
        return normalizado.equals(nome) || (categoria.usuarioId == null
                && TERMOS.getOrDefault(nome, Set.of()).contains(normalizado));
    }

    private static boolean menciona(String texto, Categoria categoria) {
        String nome = RegrasFinanceiras.normalizar(categoria.nome);
        if (nome.isEmpty()) return false;
        if ("transporte".equals(nome)) texto = UBER_EATS.matcher(texto).replaceAll(" ");
        String frase = " " + texto + " ";
        return frase.contains(" " + nome + " ") || (categoria.usuarioId == null
                && TERMOS.getOrDefault(nome, Set.of()).stream().anyMatch(termo -> frase.contains(" " + termo + " ")));
    }

    private static Categoria unica(List<Categoria> candidatas) {
        return candidatas.size() == 1 ? candidatas.getFirst() : null;
    }

    private static Set<String> termos(String lista) {
        return java.util.Arrays.stream(lista.split("\\|")).map(RegrasFinanceiras::normalizar)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
