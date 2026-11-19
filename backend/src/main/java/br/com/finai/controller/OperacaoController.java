package br.com.finai.controller;

import br.com.finai.dto.Contratos.CategoriaEntrada;
import br.com.finai.model.*;
import br.com.finai.service.OperacaoService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

@RestController
public class OperacaoController {
    private final OperacaoService operacao;

    public OperacaoController(OperacaoService operacao) {
        this.operacao = operacao;
    }

    @GetMapping("/api/admin/metricas")
    public Map<String, Object> metricas() {
        return operacao.metricas();
    }

    @GetMapping("/api/admin/contas")
    public List<Map<String, Object>> contas() {
        return operacao.contas();
    }

    @PutMapping("/api/admin/contas/{id}/bloqueio")
    public void bloquear(@PathVariable UUID id, @RequestBody Map<String, Boolean> dados) {
        operacao.bloquear(id, dados);
    }

    @GetMapping("/api/admin/categorias")
    public List<Categoria> categorias() {
        return operacao.padrao();
    }

    @PostMapping("/api/admin/categorias")
    public Categoria criar(@Valid @RequestBody CategoriaEntrada dados) {
        return operacao.criar(dados);
    }

    @PutMapping("/api/admin/categorias/{id}")
    public Categoria editar(@PathVariable UUID id, @Valid @RequestBody CategoriaEntrada dados) {
        return operacao.salvar(id, dados);
    }

    @GetMapping("/api/dpo/auditoria")
    public List<EventoAuditoria> auditoria() {
        return operacao.auditoria();
    }

    @GetMapping("/api/dpo/solicitacoes")
    public List<SolicitacaoPrivacidade> solicitacoes() {
        return operacao.solicitacoes();
    }

    @PutMapping("/api/dpo/solicitacoes/{id}")
    public void atender(@PathVariable UUID id, @RequestBody Map<String, String> dados) {
        operacao.atender(id, dados);
    }
}
