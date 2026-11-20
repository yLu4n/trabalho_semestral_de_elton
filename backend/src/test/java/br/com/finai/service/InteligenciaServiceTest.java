package br.com.finai.service;

import br.com.finai.adapter.ProvedorIa;
import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class InteligenciaServiceTest {
    ProvedorIa provedor = mock(ProvedorIa.class);
    FinanceiroService financeiro = mock(FinanceiroService.class);
    CotaIaService cotas = mock(CotaIaService.class);
    SugestaoLancamentoRepository sugestoes = mock(SugestaoLancamentoRepository.class);
    CorrecaoCategoriaRepository correcoes = mock(CorrecaoCategoriaRepository.class);
    EventoAuditoriaRepository auditoria = mock(EventoAuditoriaRepository.class);
    ObjectMapper mapper = new ObjectMapper();
    UUID usuario = UUID.randomUUID();
    Categoria categoria;
    InteligenciaService service = new InteligenciaService(provedor, financeiro, cotas, sugestoes, correcoes, mapper, auditoria);

    @BeforeEach
    void preparar() {
        categoria = new Categoria();
        categoria.nome = "Alimentação";
        categoria.tipo = TipoLancamento.DESPESA;
        when(financeiro.categorias(usuario)).thenReturn(List.of(categoria));
        when(correcoes.findTop100ByUsuarioIdOrderByCriadaEmDesc(usuario)).thenReturn(List.of());
        when(sugestoes.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void ajustaDataFuturaDeDespesaEExibeAviso() throws Exception {
        var retorno = resposta();
        ((com.fasterxml.jackson.databind.node.ObjectNode) retorno.get("campos"))
                .put("data", RegrasFinanceiras.hoje().plusDays(2).toString());
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(retorno);
        var sugestao = service.interpretar(usuario, "Paguei 38,20 na próxima sexta");
        assertThat(sugestao.campos().get("data")).isEqualTo(RegrasFinanceiras.hoje().toString());
        assertThat(sugestao.aviso()).contains("data futura", "hoje");
    }

    @Test
    void interpretaOntemSemDependerDoCalculoDoModelo() throws Exception {
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(resposta());
        var sugestao = service.interpretar(usuario, "Almocei gastando 38 reais ontem.");
        assertThat(sugestao.campos().get("data")).isEqualTo(RegrasFinanceiras.hoje().minusDays(1).toString());
        var ambigua = service.interpretar(usuario, "Hoje me lembrei da despesa de ontem.");
        assertThat(ambigua.campos().get("data")).isEqualTo(RegrasFinanceiras.hoje().toString());
    }

    @Test
    void sugereTransporteParaCorridaSemConfundirEntrega() throws Exception {
        Categoria transporte = new Categoria();
        transporte.nome = "Transporte";
        transporte.tipo = TipoLancamento.DESPESA;
        when(financeiro.categorias(usuario)).thenReturn(List.of(categoria, transporte));
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(resposta());

        var corrida = service.interpretar(usuario, "Paguei 52 reais de Uber ontem no cartão");
        assertThat(corrida.campos().get("categoriaId")).isEqualTo(transporte.id.toString());
        var entrega = service.interpretar(usuario, "Paguei 52 reais de Uber Eats ontem no cartão");
        assertThat(entrega.campos().get("categoriaId")).isEqualTo(categoria.id.toString());
    }

    @Test
    void mantemCategoriaEValorExplicitoQuandoOProvedorFalha() {
        Categoria transporte = new Categoria();
        transporte.nome = "Transporte";
        transporte.tipo = TipoLancamento.DESPESA;
        Categoria saude = new Categoria();
        saude.nome = "Saúde";
        saude.tipo = TipoLancamento.DESPESA;
        when(financeiro.categorias(usuario)).thenReturn(List.of(categoria, transporte, saude));
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenThrow(new IllegalStateException());

        for (String frase : List.of("Uber", "ônibus", "transporte")) {
            var sugestao = service.interpretar(usuario, frase);
            assertThat(sugestao.campos()).containsEntry("categoriaId", transporte.id.toString()).doesNotContainKey("valor");
            assertThat(sugestao.pendentes()).contains("valor");
        }
        var sugestao = service.interpretar(usuario, "Paguei R$ 52,30 no hospital ontem");
        assertThat(sugestao.campos()).containsEntry("categoriaId", saude.id.toString())
                .containsEntry("valor", "52.30").containsEntry("data", RegrasFinanceiras.hoje().minusDays(1).toString());
        assertThat(service.interpretar(usuario, "Fui ao hospital").campos())
                .containsEntry("categoriaId", saude.id.toString()).doesNotContainKey("valor");
    }

    JsonNode resposta() throws Exception {
        return mapper.readTree("{\"campos\":{\"tipo\":\"DESPESA\",\"valor\":38.20,\"data\":\""
                + RegrasFinanceiras.hoje() + "\",\"categoriaId\":\"" + categoria.id
                + "\",\"descricao\":\"Almoço\",\"estabelecimento\":null,\"formaPagamento\":\"Pix\"},\"confianca\":{\"tipo\":0.99,\"valor\":0.79,\"data\":0.95,\"categoriaId\":0.9,\"descricao\":0.99,\"estabelecimento\":0.1,\"formaPagamento\":0.5}}");
    }

    @Test
    void respeitaConfiancaENaoSalvaAntesDeConfirmar() throws Exception {
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(resposta());
        var s = service.interpretar(usuario, "Paguei 38,20 no almoço");
        assertThat(s.manual()).isFalse();
        assertThat(s.conferirValor()).isTrue();
        assertThat(s.pendentes()).contains("estabelecimento", "formaPagamento");
        assertThat(s.campos().get("formaPagamento")).isNull();
        verify(financeiro, never()).salvar(any(), any(), any(), any());
    }

    @Test
    void falhaTentaNovamenteEOfereceManual() {
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenThrow(new IllegalStateException());
        var s = service.interpretar(usuario, "Movimento 38");
        assertThat(s.manual()).isTrue();
        assertThat(s.campos()).containsOnlyKeys("descricao");
        verify(provedor, times(2)).gerar(any(), any(), any(), anyBoolean());
        assertThatThrownBy(() -> service.interpretar(usuario, "x".repeat(281)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaEsquemaCategoriaValorEConfianca() throws Exception {
        var r = (com.fasterxml.jackson.databind.node.ObjectNode) resposta();
        ((com.fasterxml.jackson.databind.node.ObjectNode) r.get("campos")).put("categoriaId",
                UUID.randomUUID().toString());
        assertThatThrownBy(() -> service.validar(r, List.of(categoria))).isInstanceOf(IllegalArgumentException.class);
        var invalida = (com.fasterxml.jackson.databind.node.ObjectNode) resposta();
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalida.get("confianca")).put("valor", 1.2);
        assertThatThrownBy(() -> service.validar(invalida, List.of(categoria)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validar(mapper.createObjectNode(), List.of(categoria)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmaUmaVezERegistraCorrecao() throws Exception {
        SugestaoLancamento s = new SugestaoLancamento();
        s.usuarioId = usuario;
        s.textoOriginal = "Paguei 38 no almoço";
        s.dados = mapper.writeValueAsString(Map.of("categoriaId", categoria.id.toString(),
                "valor", "37", "data", RegrasFinanceiras.hoje().toString(), "tipo", "DESPESA",
                "descricao", "Almoço", "formaPagamento", "Pix"));
        when(sugestoes.obterParaConfirmacao(s.id, usuario)).thenReturn(Optional.of(s));
        UUID nova = UUID.randomUUID();
        Lancamento l = new Lancamento();
        l.categoriaId = nova;
        l.tipo = TipoLancamento.DESPESA;
        l.valor = new BigDecimal("38.00");
        l.data = RegrasFinanceiras.hoje();
        l.descricao = "Almoço";
        l.estabelecimento = "";
        l.formaPagamento = "Pix";
        when(financeiro.salvar(eq(usuario), isNull(), any(), eq("IA"))).thenReturn(l);
        var entrada = new LancamentoEntrada(TipoLancamento.DESPESA, new BigDecimal("38"), RegrasFinanceiras.hoje(),
                nova, "Almoço", "", "Pix");
        assertThatThrownBy(() -> service.confirmar(usuario, s.id, new Confirmacao(false, entrada)))
                .isInstanceOf(RegraNegocioException.class);
        service.confirmar(usuario, s.id, new Confirmacao(true, entrada));
        verify(correcoes).save(any());
        verify(auditoria).saveAll(argThat(eventos -> {
            List<String> acoes = new ArrayList<>();
            for (EventoAuditoria evento : eventos) {
                assertThat(evento.correlacaoId).isEqualTo(s.id);
                acoes.add(evento.acao);
            }
            assertThat(acoes).containsExactlyInAnyOrder("CORRECAO_IA_valor", "CORRECAO_IA_categoriaId");
            return true;
        }));
        clearInvocations(auditoria);
        s.dados = mapper.writeValueAsString(Map.of("categoriaId", nova.toString(), "valor", "38",
                "data", l.data.toString(), "tipo", "DESPESA", "descricao", "Almoço", "formaPagamento", "Pix"));
        service.confirmar(usuario, s.id, new Confirmacao(true, entrada));
        verifyNoInteractions(auditoria);
        verify(sugestoes, times(2)).delete(s);
        service.descartar(usuario, s.id);
        s.criadaEm = Instant.now().minusSeconds(1801);
        assertThatThrownBy(() -> service.confirmar(usuario, s.id, new Confirmacao(true, entrada)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void tresCorrecoesConcordantesAplicamPreferencia() throws Exception {
        Categoria outra = new Categoria();
        outra.nome = "Lazer";
        outra.tipo = TipoLancamento.DESPESA;
        when(financeiro.categorias(usuario)).thenReturn(List.of(categoria, outra));
        List<CorrecaoCategoria> lista = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CorrecaoCategoria c = new CorrecaoCategoria();
            c.termo = "almoco";
            c.categoriaConfirmada = outra.id;
            lista.add(c);
        }
        when(correcoes.findTop100ByUsuarioIdOrderByCriadaEmDesc(usuario)).thenReturn(lista);
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(resposta());
        var s = service.interpretar(usuario, "Paguei 38 no almoço");
        assertThat(s.campos().get("categoriaId")).isEqualTo(outra.id.toString());
    }

    @Test
    void diagnosticoUsaApenasAgregadosEValidaResposta() throws Exception {
        var variacao = new Variacao("Alimentação", new BigDecimal("10"), new BigDecimal("30"), new BigDecimal("20"));
        when(financeiro.variacoes(eq(usuario), any())).thenReturn(List.of(variacao));
        when(provedor.gerar(eq("DIAGNOSTICO"), any(), any(), eq(false)))
                .thenReturn(mapper.readTree("{\"texto\":\"O maior destaque acompanha a tendência de aumento.\"}"));
        var d = service.diagnosticar(usuario, YearMonth.now().toString());
        assertThat(d.texto()).contains("R$ 10,00", "R$ 30,00", "O maior destaque acompanha a tendência de aumento.");
        assertThat(d.variacoes()).containsExactly(variacao);
        verifyNoInteractions(cotas);
        assertThat(d.aviso()).contains("não constitui");
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(provedor).gerar(eq("DIAGNOSTICO"), any(), captor.capture(), eq(false));
        assertThat(mapper.writeValueAsString(captor.getValue())).doesNotContain(usuario.toString(), "Alimentação",
                "descricao");
        when(provedor.gerar(any(), any(), any(), anyBoolean())).thenReturn(mapper.createObjectNode());
        assertThatThrownBy(() -> service.diagnosticar(usuario, YearMonth.now().toString()))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void diagnosticoSemDadosENoFuturo() {
        when(financeiro.variacoes(eq(usuario), any())).thenReturn(List.of());
        assertThat(service.diagnosticar(usuario, YearMonth.now().toString()).variacoes()).isEmpty();
        assertThatThrownBy(() -> service.diagnosticar(usuario, YearMonth.now().plusMonths(1).toString()))
                .isInstanceOf(RegraNegocioException.class);
    }
}
