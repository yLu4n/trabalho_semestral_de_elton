package br.com.finai.service;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class FinanceiroService {
    private final LancamentoRepository lancamentos;
    private final CategoriaRepository categorias;
    private final MetaOrcamentoRepository metas;
    public FinanceiroService(LancamentoRepository lancamentos,CategoriaRepository categorias,MetaOrcamentoRepository metas) {
        this.lancamentos=lancamentos; this.categorias=categorias; this.metas=metas;
    }
    public List<Categoria> categorias(UUID usuario) { return categorias.disponiveis(usuario); }
    public Categoria categoria(UUID usuario,UUID id,boolean exigirAtiva) {
        Categoria c=categorias.findById(id).filter(x->x.usuarioId==null || x.usuarioId.equals(usuario)).orElseThrow(()->new RegraNegocioException(404,"Categoria não encontrada."));
        if(exigirAtiva && !c.ativa) throw new RegraNegocioException("Escolha uma categoria ativa.");
        return c;
    }
    @Transactional public Categoria salvarCategoria(UUID usuario,UUID id,CategoriaEntrada dados) {
        Categoria c=id==null?new Categoria():categoria(usuario,id,false);
        if(id!=null && !usuario.equals(c.usuarioId)) throw new RegraNegocioException(403,"Categorias padrão são gerenciadas pelo administrador.");
        if(id!=null && c.tipo!=dados.tipo() && lancamentos.existsByCategoriaIdAndExcluidoEmIsNull(c.id)) throw new RegraNegocioException("Não altere o tipo de uma categoria em uso.");
        c.usuarioId=usuario; c.nome=dados.nome().trim(); c.cor=dados.cor(); c.tipo=dados.tipo(); c.ativa=dados.ativa(); return categorias.save(c);
    }
    @Transactional public Lancamento salvar(UUID usuario,UUID id,LancamentoEntrada dados,String origem) {
        Lancamento l=id==null?new Lancamento():obter(usuario,id);
        if(l.excluidoEm!=null) throw new RegraNegocioException("Restaure o lançamento antes de editar.");
        if(id!=null) RegrasFinanceiras.dataEditavel(l.data);
        RegrasFinanceiras.dataEditavel(dados.data());
        Categoria c=categoria(usuario,dados.categoriaId(),id==null || !dados.categoriaId().equals(l.categoriaId));
        if(c.tipo!=dados.tipo()) throw new RegraNegocioException("O tipo da categoria deve coincidir com o lançamento.");
        l.usuarioId=usuario; l.tipo=dados.tipo(); l.valor=RegrasFinanceiras.moeda(dados.valor()); l.data=dados.data();
        l.categoriaId=c.id; l.descricao=dados.descricao().trim(); l.estabelecimento=dados.estabelecimento(); l.formaPagamento=dados.formaPagamento();
        if(id==null) l.origem=origem;
        return lancamentos.save(l);
    }
    public Lancamento obter(UUID usuario,UUID id) {
        return lancamentos.findByIdAndUsuarioId(id,usuario).orElseThrow(()->new RegraNegocioException(404,"Lançamento não encontrado."));
    }
    @Transactional public void excluir(UUID usuario,UUID id) {
        Lancamento l=obter(usuario,id); RegrasFinanceiras.dataEditavel(l.data);
        if(l.excluidoEm==null) { l.excluidoEm=Instant.now(); lancamentos.save(l); }
    }
    @Transactional public void restaurar(UUID usuario,UUID id) {
        Lancamento l=obter(usuario,id); RegrasFinanceiras.dataEditavel(l.data);
        if(l.excluidoEm==null || l.excluidoEm.plusSeconds(90L*86400).isBefore(Instant.now())) throw new RegraNegocioException("O prazo de recuperação expirou.");
        l.excluidoEm=null; lancamentos.save(l);
    }
    public List<Lancamento> lixeira(UUID usuario) { return lancamentos.findByUsuarioIdAndExcluidoEmIsNotNull(usuario); }
    public List<Lancamento> filtrar(UUID usuario,Filtro filtro) {
        return filtrarItens(lancamentos.findByUsuarioIdAndExcluidoEmIsNullOrderByDataDescCriadoEmDesc(usuario),filtro);
    }
    private List<Lancamento> filtrarItens(List<Lancamento> historico,Filtro filtro) {
        if(filtro.inicio()!=null && filtro.fim()!=null && filtro.inicio().isAfter(filtro.fim())) throw new RegraNegocioException("O início deve ser anterior ao fim.");
        String texto=RegrasFinanceiras.normalizar(filtro.texto());
        return historico.stream()
            .filter(l->filtro.inicio()==null || !l.data.isBefore(filtro.inicio()))
            .filter(l->filtro.fim()==null || !l.data.isAfter(filtro.fim()))
            .filter(l->filtro.categoriaId()==null || l.categoriaId.equals(filtro.categoriaId()))
            .filter(l->filtro.tipo()==null || l.tipo==filtro.tipo())
            .filter(l->texto.isEmpty() || RegrasFinanceiras.normalizar(l.descricao).contains(texto)).toList();
    }
    public Pagina listar(UUID usuario,Filtro filtro,int pagina,int tamanho) {
        if(pagina<0 || tamanho<1 || tamanho>100) throw new RegraNegocioException("Paginação inválida.");
        List<Lancamento> todos=filtrar(usuario,filtro);
        int inicio=(int)Math.min((long)pagina*tamanho,todos.size());
        return new Pagina(todos.subList(inicio,Math.min(inicio+tamanho,todos.size())),pagina,(todos.size()+tamanho-1)/tamanho,todos.size(),RegrasFinanceiras.totais(todos));
    }
    public List<Lancamento> doMes(UUID usuario,YearMonth mes) { return filtrar(usuario,new Filtro(mes.atDay(1),mes.atEndOfMonth(),null,null,null)); }
    public Painel painel(UUID usuario,Filtro filtro) {
        var historico=lancamentos.findByUsuarioIdAndExcluidoEmIsNullOrderByDataDescCriadoEmDesc(usuario);
        var itens=filtrarItens(historico,filtro); var totais=RegrasFinanceiras.totais(itens);
        var categoriasPorId=categorias(usuario).stream().collect(Collectors.toMap(c->c.id,c->c));
        Map<UUID,BigDecimal> soma=new HashMap<>();
        itens.stream().filter(l->l.tipo==TipoLancamento.DESPESA).forEach(l->soma.merge(l.categoriaId,l.valor,BigDecimal::add));
        List<Distribuicao> distribuicao=soma.entrySet().stream().sorted(Map.Entry.<UUID,BigDecimal>comparingByValue().reversed())
            .map(e->{ Categoria c=categoriasPorId.get(e.getKey()); return new Distribuicao(c.id,c.nome,c.cor,e.getValue(),RegrasFinanceiras.percentual(e.getValue(),totais.despesas())); }).toList();
        YearMonth referencia=YearMonth.from(filtro.fim()==null?RegrasFinanceiras.hoje():filtro.fim());
        List<MesSerie> serie=new ArrayList<>();
        for(int i=5;i>=0;i--) { YearMonth mes=referencia.minusMonths(i); var t=RegrasFinanceiras.totais(filtrarItens(historico,new Filtro(mes.atDay(1),mes.atEndOfMonth(),null,null,null))); serie.add(new MesSerie(mes.toString(),t.receitas(),t.despesas())); }
        LocalDate hoje=RegrasFinanceiras.hoje(); YearMonth corrente=YearMonth.from(hoje);
        BigDecimal gasto=RegrasFinanceiras.somar(filtrarItens(historico,new Filtro(corrente.atDay(1),hoje,null,null,null)),TipoLancamento.DESPESA);
        BigDecimal projecao=gasto.multiply(BigDecimal.valueOf(corrente.lengthOfMonth())).divide(BigDecimal.valueOf(hoje.getDayOfMonth()),2,RoundingMode.HALF_UP);
        return new Painel(totais,distribuicao,serie,projecao,corrente.toString());
    }
    @Transactional public MetaOrcamento salvarMeta(UUID usuario,MetaEntrada dados) {
        Categoria c=categoria(usuario,dados.categoriaId(),true);
        if(c.tipo!=TipoLancamento.DESPESA) throw new RegraNegocioException("Metas são definidas para categorias de despesa.");
        YearMonth mes=YearMonth.parse(dados.mes());
        MetaOrcamento m=metas.findByUsuarioIdAndCategoriaIdAndMes(usuario,c.id,mes.toString()).orElseGet(MetaOrcamento::new);
        m.usuarioId=usuario; m.categoriaId=c.id; m.mes=mes.toString(); m.limite=RegrasFinanceiras.moeda(dados.limite()); return metas.save(m);
    }
    @Transactional public void excluirMeta(UUID usuario,UUID id) { metas.delete(metas.findByIdAndUsuarioId(id,usuario).orElseThrow(()->new RegraNegocioException(404,"Meta não encontrada."))); }
    public List<MetaVista> metas(UUID usuario,String mes) {
        var itens=doMes(usuario,YearMonth.parse(mes));
        return metas.findByUsuarioIdAndMes(usuario,mes).stream().map(m->{
            Categoria c=categoria(usuario,m.categoriaId,false);
            BigDecimal gasto=RegrasFinanceiras.somar(itens.stream().filter(l->l.categoriaId.equals(c.id)).toList(),TipoLancamento.DESPESA);
            BigDecimal percentual=RegrasFinanceiras.percentual(gasto,m.limite);
            return new MetaVista(m.id,c.id,c.nome,c.cor,m.mes,m.limite,gasto,percentual,RegrasFinanceiras.estadoMeta(percentual));
        }).toList();
    }
    public String exportar(UUID usuario) {
        StringBuilder csv=new StringBuilder("data,tipo,valor,categoria,descricao,estabelecimento,forma_pagamento,origem\r\n");
        for(Lancamento l:filtrar(usuario,new Filtro(null,null,null,null,null))) {
            Categoria c=categoria(usuario,l.categoriaId,false);
            csv.append(l.data).append(',').append(l.tipo).append(',').append(l.valor.toPlainString()).append(',')
                .append(RegrasFinanceiras.csv(c.nome)).append(',').append(RegrasFinanceiras.csv(l.descricao)).append(',')
                .append(RegrasFinanceiras.csv(l.estabelecimento)).append(',').append(RegrasFinanceiras.csv(l.formaPagamento)).append(',').append(l.origem).append("\r\n");
        } return csv.toString();
    }
    public List<Variacao> variacoes(UUID usuario,YearMonth mes) {
        var atual=doMes(usuario,mes); var anterior=doMes(usuario,mes.minusMonths(1));
        return categorias(usuario).stream().filter(c->c.tipo==TipoLancamento.DESPESA).map(c->{
            var a=RegrasFinanceiras.somar(atual.stream().filter(l->l.categoriaId.equals(c.id)).toList(),TipoLancamento.DESPESA);
            var b=RegrasFinanceiras.somar(anterior.stream().filter(l->l.categoriaId.equals(c.id)).toList(),TipoLancamento.DESPESA);
            return new Variacao(c.nome,b,a,a.subtract(b));
        }).filter(v->v.anterior().signum()!=0 || v.atual().signum()!=0)
            .sorted(Comparator.comparing((Variacao v)->v.diferenca().abs()).reversed()).toList();
    }
}
