package br.com.finai.service;

import br.com.finai.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class RegrasFinanceirasTest {
    @Test
    void aceitaLimitesExatos() {
        assertThat(RegrasFinanceiras.moeda(new BigDecimal("0.01"))).isEqualByComparingTo("0.01");
        assertThat(RegrasFinanceiras.moeda(new BigDecimal("1000000"))).isEqualByComparingTo("1000000.00");
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "1000000.01", "0.001" })
    void rejeitaValoresInvalidos(String valor) {
        assertThatThrownBy(() -> RegrasFinanceiras.moeda(new BigDecimal(valor)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void calculaTotaisSemPontoFlutuanteEIgnoraExcluidos() {
        Lancamento a = new Lancamento();
        a.tipo = TipoLancamento.RECEITA;
        a.valor = new BigDecimal("0.30");
        Lancamento b = new Lancamento();
        b.tipo = TipoLancamento.DESPESA;
        b.valor = new BigDecimal("0.10");
        Lancamento c = new Lancamento();
        c.tipo = TipoLancamento.DESPESA;
        c.valor = new BigDecimal("999");
        c.excluidoEm = java.time.Instant.now();
        var totais = RegrasFinanceiras.totais(List.of(a, b, c));
        assertThat(totais.saldo()).isEqualByComparingTo("0.20");
    }

    @Test
    void metasRespeitamFronteiras() {
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("79.99"), new BigDecimal("100"))).isEqualTo("REGULAR");
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("80"), new BigDecimal("100"))).isEqualTo("ATENCAO");
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("100"), new BigDecimal("100"))).isEqualTo("ATENCAO");
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("100.01"), new BigDecimal("100"))).isEqualTo("CRITICO");
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("799999.99"), new BigDecimal("1000000"))).isEqualTo("REGULAR");
        assertThat(RegrasFinanceiras.estadoMeta(new BigDecimal("1000000.01"), new BigDecimal("1000000"))).isEqualTo("CRITICO");
        assertThat(RegrasFinanceiras.percentual(BigDecimal.ONE, BigDecimal.ZERO)).isZero();
    }

    @Test
    void historicoEhSomenteConsulta() {
        RegrasFinanceiras.dataEditavel(RegrasFinanceiras.hoje().minusMonths(24));
        assertThatThrownBy(() -> RegrasFinanceiras.dataEditavel(RegrasFinanceiras.hoje().minusMonths(24).minusDays(1)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void protegeCsvENormalizaTermos() {
        assertThat(RegrasFinanceiras.csv("=HYPERLINK(1)")).startsWith("\"'");
        assertThat(RegrasFinanceiras.csv("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(RegrasFinanceiras.csv(null)).isEqualTo("\"\"");
        assertThat(RegrasFinanceiras.termo("Paguei 38 reais no CAFÉ ontem via Pix")).isEqualTo("cafe");
        assertThat(RegrasFinanceiras.normalizar(null)).isEmpty();
    }

    @Test
    void politicaDeSenha() {
        AutenticacaoService.validarSenha("SeguraSenha123!");
        AutenticacaoService.validarSenha("abc12345");
        AutenticacaoService.validarSenha("cafés123");
        for (String senha : List.of("curta1", "12345678!", "SemNumeros!!!!!", "a1".repeat(37))) {
            assertThatThrownBy(() -> AutenticacaoService.validarSenha(senha)).isInstanceOf(RegraNegocioException.class);
        }
    }
}
