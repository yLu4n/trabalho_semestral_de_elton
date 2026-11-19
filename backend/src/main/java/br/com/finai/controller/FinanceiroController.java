package br.com.finai.controller;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.service.FinanceiroService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FinanceiroController {
    private final FinanceiroService financeiro;

    public FinanceiroController(FinanceiroService financeiro) {
        this.financeiro = financeiro;
    }

    @GetMapping("/categorias")
    public List<Categoria> categorias(@AuthenticationPrincipal Usuario u) {
        return financeiro.categorias(u.id);
    }

    @PostMapping("/categorias")
    public Categoria criarCategoria(@AuthenticationPrincipal Usuario u, @Valid @RequestBody CategoriaEntrada d) {
        return financeiro.salvarCategoria(u.id, null, d);
    }

    @PutMapping("/categorias/{id}")
    public Categoria editarCategoria(@AuthenticationPrincipal Usuario u, @PathVariable UUID id,
            @Valid @RequestBody CategoriaEntrada d) {
        return financeiro.salvarCategoria(u.id, id, d);
    }

    @PostMapping("/lancamentos")
    public Lancamento criar(@AuthenticationPrincipal Usuario u, @Valid @RequestBody LancamentoEntrada d) {
        return financeiro.salvar(u.id, null, d, "MANUAL");
    }

    @PutMapping("/lancamentos/{id}")
    public Lancamento editar(@AuthenticationPrincipal Usuario u, @PathVariable UUID id,
            @Valid @RequestBody LancamentoEntrada d) {
        return financeiro.salvar(u.id, id, d, "MANUAL");
    }

    @DeleteMapping("/lancamentos/{id}")
    public void excluir(@AuthenticationPrincipal Usuario u, @PathVariable UUID id) {
        financeiro.excluir(u.id, id);
    }

    @PostMapping("/lancamentos/{id}/restaurar")
    public void restaurar(@AuthenticationPrincipal Usuario u, @PathVariable UUID id) {
        financeiro.restaurar(u.id, id);
    }

    @GetMapping("/lixeira")
    public List<Lancamento> lixeira(@AuthenticationPrincipal Usuario u) {
        return financeiro.lixeira(u.id);
    }

    @GetMapping("/lancamentos")
    public Pagina listar(@AuthenticationPrincipal Usuario u,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @RequestParam(required = false) UUID categoriaId, @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) String texto, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return financeiro.listar(u.id, new Filtro(inicio, fim, categoriaId, tipo, texto), pagina, tamanho);
    }

    @GetMapping("/painel")
    public Painel painel(@AuthenticationPrincipal Usuario u, @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim) {
        return financeiro.painel(u.id, new Filtro(inicio, fim, null, null, null));
    }

    @GetMapping("/metas")
    public List<MetaVista> metas(@AuthenticationPrincipal Usuario u, @RequestParam String mes) {
        return financeiro.metas(u.id, mes);
    }

    @PostMapping("/metas")
    public MetaOrcamento salvarMeta(@AuthenticationPrincipal Usuario u, @Valid @RequestBody MetaEntrada d) {
        return financeiro.salvarMeta(u.id, d);
    }

    @DeleteMapping("/metas/{id}")
    public void excluirMeta(@AuthenticationPrincipal Usuario u, @PathVariable UUID id) {
        financeiro.excluirMeta(u.id, id);
    }

    @GetMapping(value = "/exportacao", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Usuario u) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finai-extrato.csv")
                .body(financeiro.exportar(u.id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
