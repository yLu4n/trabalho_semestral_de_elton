package br.com.finai.controller;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.Usuario;
import br.com.finai.service.AutenticacaoService;
import br.com.finai.config.SegurancaConfig;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private final AutenticacaoService auth;
    private final boolean seguro;

    public AutenticacaoController(AutenticacaoService auth, @Value("${finai.cookie-secure}") boolean seguro) {
        this.auth = auth;
        this.seguro = seguro;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/cadastro")
    public Map<String, String> cadastrar(@Valid @RequestBody Cadastro dados) {
        auth.cadastrar(dados);
        return Map.of("mensagem", "Conta criada. Você já pode entrar e receberá um link para verificar o e-mail.");
    }

    @PostMapping("/verificar")
    public Map<String, String> verificar(@RequestBody Map<String, String> body) {
        auth.verificar(body.getOrDefault("token", ""));
        return Map.of("mensagem", "E-mail verificado. Você já pode entrar.");
    }

    @PostMapping("/recuperar")
    public Map<String, String> recuperar(@Valid @RequestBody RecuperacaoSenha dados) {
        auth.solicitarRecuperacao(dados);
        return Map.of("mensagem", "Se a conta estiver disponível, enviaremos um vínculo de recuperação.");
    }

    @PostMapping("/redefinir")
    public Map<String, String> redefinir(@Valid @RequestBody RedefinicaoSenha dados) {
        auth.redefinirSenha(dados);
        return Map.of("mensagem", "Senha redefinida. Entre com a nova senha.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> entrar(@Valid @RequestBody Login dados) {
        String token = auth.entrar(dados);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(token, 43200))
                .body(Map.of("mensagem", "Sessão iniciada."));
    }

    @GetMapping("/eu")
    public Perfil eu(@AuthenticationPrincipal Usuario u) {
        return Perfil.de(u);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> sair(HttpServletRequest req) {
        String token = SegurancaConfig.cookie(req, "FINAI_SESSION");
        if (token != null)
            auth.sair(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", 0))
                .body(Map.of("mensagem", "Sessão encerrada."));
    }

    private String cookie(String token, long segundos) {
        return ResponseCookie.from("FINAI_SESSION", token).httpOnly(true).secure(seguro).sameSite("Strict").path("/")
                .maxAge(segundos).build().toString();
    }
}
