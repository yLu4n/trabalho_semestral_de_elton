package br.com.finai.controller;

import br.com.finai.dto.Contratos.Senha;
import br.com.finai.model.*;
import br.com.finai.service.PrivacidadeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/privacidade")
public class PrivacidadeController {
    private final PrivacidadeService privacidade;

    public PrivacidadeController(PrivacidadeService privacidade) {
        this.privacidade = privacidade;
    }

    @PostMapping("/exclusao")
    public SolicitacaoPrivacidade excluir(@AuthenticationPrincipal Usuario u, @Valid @RequestBody Senha senha) {
        return privacidade.solicitarExclusao(u, senha.senha());
    }

    @GetMapping("/solicitacoes")
    public List<SolicitacaoPrivacidade> minhas(@AuthenticationPrincipal Usuario u) {
        return privacidade.minhas(u.id);
    }

    @PostMapping("/solicitacoes")
    public SolicitacaoPrivacidade solicitar(@AuthenticationPrincipal Usuario u, @RequestBody Map<String, String> d) {
        return privacidade.solicitar(u.id, d.getOrDefault("tipo", ""));
    }
}
