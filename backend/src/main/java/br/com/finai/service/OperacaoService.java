package br.com.finai.service;

import br.com.finai.dto.Contratos.CategoriaEntrada;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import br.com.finai.service.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.util.*;

@Service
public class OperacaoService {
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final CategoriaRepository categorias;
    private final RegistroInferenciaRepository inferencias;
    private final CorrecaoCategoriaRepository correcoes;
    private final EventoAuditoriaRepository auditoria;
    private final SolicitacaoPrivacidadeRepository solicitacoes;

    public OperacaoService(UsuarioRepository usuarios, SessaoRepository sessoes, CategoriaRepository categorias,
            RegistroInferenciaRepository inferencias,
            CorrecaoCategoriaRepository correcoes, EventoAuditoriaRepository auditoria,
            SolicitacaoPrivacidadeRepository solicitacoes) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.categorias = categorias;
        this.inferencias = inferencias;
        this.correcoes = correcoes;
        this.auditoria = auditoria;
        this.solicitacoes = solicitacoes;
    }

    public Map<String, Object> metricas() {
        var registros = inferencias.findAll(PageRequest.of(0, 1000, Sort.by("criadoEm").descending())).getContent();
        return Map.of("inferencias", inferencias.count(), "correcoes", correcoes.count(), "amostra", registros,
                "observacao",
                "Correções são um indicador; a acurácia exige avaliação em conjunto rotulado. Custos são estimativas conforme preços configurados.");
    }

    public List<Map<String, Object>> contas() {
        return usuarios.findAll().stream()
                .map(u -> Map.<String, Object>of("id", u.id, "papel", u.papel, "bloqueado", u.bloqueado)).toList();
    }

    @Transactional
    public void bloquear(UUID id, Map<String, Boolean> d) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new RegraNegocioException(404, "Conta não encontrada."));
        if (u.papel != Papel.TITULAR)
            throw new RegraNegocioException("A operação se aplica a titulares.");
        u.bloqueado = Boolean.TRUE.equals(d.get("bloqueado"));
        usuarios.save(u);
        sessoes.deleteByUsuarioId(id);
    }

    public List<Categoria> padrao() {
        return categorias.disponiveis(new UUID(0, 0));
    }

    public Categoria criar(CategoriaEntrada d) {
        return salvar(null, d);
    }

    public Categoria salvar(UUID id, CategoriaEntrada d) {
        Categoria c = id == null ? new Categoria()
                : categorias.findById(id).filter(x -> x.usuarioId == null)
                        .orElseThrow(() -> new RegraNegocioException(404, "Categoria padrão não encontrada."));
        if (id != null && c.tipo != d.tipo())
            throw new RegraNegocioException("O tipo de uma categoria padrão é imutável.");
        c.nome = d.nome().trim();
        c.tipo = d.tipo();
        c.cor = d.cor();
        c.ativa = d.ativa();
        return categorias.save(c);
    }

    public List<EventoAuditoria> auditoria() {
        return auditoria.findAll(PageRequest.of(0, 200, Sort.by("criadoEm").descending())).getContent();
    }

    public List<SolicitacaoPrivacidade> solicitacoes() {
        return solicitacoes.findAll(PageRequest.of(0, 200, Sort.by("criadaEm").descending())).getContent();
    }

    public void atender(UUID id, Map<String, String> d) {
        String estado = d.get("estado");
        if (!Set.of("ABERTA", "EM_ATENDIMENTO", "CONCLUIDA").contains(estado))
            throw new RegraNegocioException("Estado inválido.");
        SolicitacaoPrivacidade s = solicitacoes.findById(id)
                .orElseThrow(() -> new RegraNegocioException(404, "Solicitação não encontrada."));
        s.estado = estado;
        solicitacoes.save(s);
    }
}
