package br.com.finai.service;

import br.com.finai.model.*;
import br.com.finai.dto.Contratos.*;
import java.math.*;
import java.time.*;
import java.text.Normalizer;
import java.util.*;

public final class RegrasFinanceiras {
    public static final ZoneId FUSO = ZoneId.of("America/Bahia");

    private RegrasFinanceiras() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public static BigDecimal moeda(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(new BigDecimal("1000000")) > 0)
            throw new RegraNegocioException("O valor deve estar entre R$ 0,01 e R$ 1.000.000,00.");
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new RegraNegocioException("Informe no máximo duas casas decimais.");
        }
    }

    public static void dataEditavel(LocalDate data) {
        if (data == null || data.isBefore(hoje().minusMonths(24)))
            throw new RegraNegocioException("Registros anteriores a 24 meses são somente para consulta.");
    }

    public static BigDecimal somar(List<Lancamento> itens, TipoLancamento tipo) {
        return itens.stream().filter(l -> l.excluidoEm == null && l.tipo == tipo).map(l -> l.valor)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    public static Totais totais(List<Lancamento> itens) {
        var receitas = somar(itens, TipoLancamento.RECEITA);
        var despesas = somar(itens, TipoLancamento.DESPESA);
        return new Totais(receitas, despesas, receitas.subtract(despesas));
    }

    public static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO
                : parte.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    public static String estadoMeta(BigDecimal consumido, BigDecimal limite) {
        if (consumido.compareTo(limite) > 0) return "CRITICO";
        return consumido.compareTo(limite.multiply(new BigDecimal("0.80"))) >= 0 ? "ATENCAO" : "REGULAR";
    }

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static String termo(String texto) {
        return normalizar(texto).replaceAll(
                "\\b(hoje|ontem|reais|real|paguei|gastei|comprei|no|na|em|de|do|da|o|a|um|uma|por|via|pix|dinheiro|cartao)\\b",
                "")
                .replaceAll("\\d+", "").replaceAll("\\s+", " ").trim();
    }

    public static String csv(String texto) {
        String seguro = texto == null ? "" : texto;
        if (seguro.stripLeading().matches("^[=+@\\-\\t\\r].*"))
            seguro = "'" + seguro;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }
}
