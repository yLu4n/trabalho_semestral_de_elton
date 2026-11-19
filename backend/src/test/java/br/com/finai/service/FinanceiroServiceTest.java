package br.com.finai.service;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.junit.jupiter.api.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class FinanceiroServiceTest {
    LancamentoRepository registros = mock(LancamentoRepository.class);
    CategoriaRepository categorias = mock(CategoriaRepository.class);
    MetaOrcamentoRepository metas = mock(MetaOrcamentoRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    Usuario titular = new Usuario();
    FinanceiroService service = new FinanceiroService(registros, categorias, metas, usuarios);
    UUID usuario = UUID.randomUUID();
    Categoria categoria;
    Lancamento lancamento;

    @BeforeEach
    void preparar() {
        titular.emailVerificado = true;
        when(usuarios.bloquearParaAtualizacao(usuario)).thenReturn(Optional.of(titular));
        categoria = new Categoria();
        categoria.nome = "Alimentação";
        categoria.tipo = TipoLancamento.DESPESA;
        categoria.cor = "#123456";
        when(categorias.findById(categoria.id)).thenReturn(Optional.of(categoria));
        when(categorias.disponiveis(usuario)).thenReturn(List.of(categoria));
        lancamento = new Lancamento();
        lancamento.usuarioId = usuario;
        lancamento.categoriaId = categoria.id;
        lancamento.tipo = TipoLancamento.DESPESA;
        lancamento.data = RegrasFinanceiras.hoje();
        lancamento.descricao = "Café";
        lancamento.valor = new BigDecimal("10.50");
        when(registros.findByUsuarioIdAndExcluidoEmIsNullOrderByDataDescCriadoEmDesc(usuario))
                .thenReturn(List.of(lancamento));
        when(registros.findByIdAndUsuarioId(lancamento.id, usuario)).thenReturn(Optional.of(lancamento));
        when(registros.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categorias.save(any())).thenAnswer(i -> i.getArgument(0));
        when(metas.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void limitaCadastroPendenteASomenteCincoLancamentos() {
        titular.emailVerificado = false;
        for (int i = 0; i < 5; i++)
            service.salvar(usuario, null, entrada(), i == 0 ? "IA" : "MANUAL");
        assertThat(titular.lancamentosExperimentais).isEqualTo(5);
        assertThatThrownBy(() -> service.salvar(usuario, null, entrada(), "MANUAL"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("cinco");
        titular.emailVerificado = true;
        service.salvar(usuario, null, entrada(), "MANUAL");
        assertThat(titular.lancamentosExperimentais).isEqualTo(5);
    }

    LancamentoEntrada entrada() {
        return new LancamentoEntrada(TipoLancamento.DESPESA, new BigDecimal("38.20"), RegrasFinanceiras.hoje(),
                categoria.id, "Almoço", "Restaurante", "Pix");
    }

    @Test
    void salvaManualEEdita() {
        var salvo = service.salvar(usuario, null, entrada(), "MANUAL");
        assertThat(salvo.valor).isEqualByComparingTo("38.20");
        assertThat(salvo.usuarioId).isEqualTo(usuario);
        service.salvar(usuario, lancamento.id, entrada(), "MANUAL");
        assertThat(lancamento.descricao).isEqualTo("Almoço");
    }

    @Test
    void impedeAcessoHorizontal() {
        assertThatThrownBy(() -> service.obter(UUID.randomUUID(), lancamento.id))
                .isInstanceOf(RegraNegocioException.class);
        categoria.usuarioId = UUID.randomUUID();
        assertThatThrownBy(() -> service.categoria(usuario, categoria.id, false))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaCategoriaInativaOuTipoIncompativel() {
        categoria.ativa = false;
        assertThatThrownBy(() -> service.salvar(usuario, null, entrada(), "MANUAL"))
                .isInstanceOf(RegraNegocioException.class);
        categoria.ativa = true;
        categoria.tipo = TipoLancamento.RECEITA;
        assertThatThrownBy(() -> service.salvar(usuario, null, entrada(), "MANUAL"))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void exclusaoLogicaRestauracaoEHistorico() {
        service.excluir(usuario, lancamento.id);
        assertThat(lancamento.excluidoEm).isNotNull();
        assertThatThrownBy(() -> service.salvar(usuario, lancamento.id, entrada(), "MANUAL"))
                .isInstanceOf(RegraNegocioException.class);
        service.restaurar(usuario, lancamento.id);
        assertThat(lancamento.excluidoEm).isNull();
        assertThatThrownBy(() -> service.restaurar(usuario, lancamento.id)).isInstanceOf(RegraNegocioException.class);
        lancamento.data = RegrasFinanceiras.hoje().minusMonths(25);
        assertThatThrownBy(() -> service.excluir(usuario, lancamento.id)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void filtrosEPaginacaoConservamTotais() {
        var hoje = RegrasFinanceiras.hoje();
        var pagina = service.listar(usuario, new Filtro(hoje, hoje, categoria.id, TipoLancamento.DESPESA, "cafe"), 0,
                1);
        assertThat(pagina.quantidade()).isEqualTo(1);
        assertThat(pagina.totais().despesas()).isEqualByComparingTo("10.50");
        assertThat(service.listar(usuario, new Filtro(null, null, null, null, "inexistente"), 0, 10).itens()).isEmpty();
        assertThat(service.listar(usuario, new Filtro(null, null, null, null, null), 50, 10).itens()).isEmpty();
        assertThatThrownBy(() -> service.listar(usuario, new Filtro(null, null, null, null, null), -1, 10))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.filtrar(usuario, new Filtro(hoje, hoje.minusDays(1), null, null, null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void painelETotaisSaoCoerentes() {
        var p = service.painel(usuario, new Filtro(null, null, null, null, null));
        assertThat(p.totais().despesas()).isEqualByComparingTo("10.50");
        assertThat(p.distribuicao().getFirst().percentual()).isEqualByComparingTo("100");
        assertThat(p.serie()).hasSize(6);
        assertThat(p.projecaoDespesas()).isPositive();
    }

    @Test
    void categoriasPadraoSaoProtegidas() {
        var d = new CategoriaEntrada("Nova", TipoLancamento.DESPESA, "#112233", true);
        assertThat(service.salvarCategoria(usuario, null, d).usuarioId).isEqualTo(usuario);
        assertThatThrownBy(() -> service.salvarCategoria(usuario, categoria.id, d))
                .isInstanceOf(RegraNegocioException.class);
        categoria.usuarioId = usuario;
        assertThat(service.salvarCategoria(usuario, categoria.id, d).nome).isEqualTo("Nova");
        when(registros.existsByCategoriaIdAndExcluidoEmIsNull(categoria.id)).thenReturn(true);
        assertThatThrownBy(() -> service.salvarCategoria(usuario, categoria.id,
                new CategoriaEntrada("X", TipoLancamento.RECEITA, "#123456", true)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void metasCalculamConsumoEEstado() {
        MetaOrcamento meta = new MetaOrcamento();
        meta.usuarioId = usuario;
        meta.categoriaId = categoria.id;
        meta.mes = YearMonth.now().toString();
        meta.limite = new BigDecimal("10");
        when(metas.findByUsuarioIdAndMes(usuario, meta.mes)).thenReturn(List.of(meta));
        when(metas.findByUsuarioIdAndCategoriaIdAndMes(usuario, categoria.id, meta.mes)).thenReturn(Optional.of(meta));
        assertThat(service.metas(usuario, meta.mes).getFirst().estado()).isEqualTo("CRITICO");
        assertThat(service.salvarMeta(usuario, new MetaEntrada(categoria.id, meta.mes, new BigDecimal("20"))).limite)
                .isEqualByComparingTo("20");
        when(metas.findByIdAndUsuarioId(meta.id, usuario)).thenReturn(Optional.of(meta));
        service.excluirMeta(usuario, meta.id);
        verify(metas).delete(meta);
        categoria.tipo = TipoLancamento.RECEITA;
        assertThatThrownBy(() -> service.salvarMeta(usuario, new MetaEntrada(categoria.id, meta.mes, BigDecimal.ONE)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void exportaUtf8EAgregaVariacoes() {
        String csv = service.exportar(usuario);
        assertThat(csv).contains("Alimentação", "10.50", "Café");
        assertThat(service.variacoes(usuario, YearMonth.from(lancamento.data)).getFirst().diferenca())
                .isEqualByComparingTo("10.50");
        when(registros.findByUsuarioIdAndExcluidoEmIsNotNull(usuario)).thenReturn(List.of(lancamento));
        assertThat(service.lixeira(usuario)).hasSize(1);
    }
}
