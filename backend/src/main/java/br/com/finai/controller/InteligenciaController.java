package br.com.finai.controller;
import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.service.InteligenciaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.*;
@RestController @RequestMapping("/api/ia")
public class InteligenciaController {
    private final InteligenciaService ia;
    public InteligenciaController(InteligenciaService ia) { this.ia=ia; }
    @PostMapping("/interpretacoes") public SugestaoVista interpretar(@AuthenticationPrincipal Usuario u,@Valid @RequestBody TextoEntrada texto) { return ia.interpretar(u.id,texto.texto()); }
    @PostMapping("/sugestoes/{id}/confirmar") public Lancamento confirmar(@AuthenticationPrincipal Usuario u,@PathVariable UUID id,@Valid @RequestBody Confirmacao d) { return ia.confirmar(u.id,id,d); }
    @DeleteMapping("/sugestoes/{id}") public void descartar(@AuthenticationPrincipal Usuario u,@PathVariable UUID id) { ia.descartar(u.id,id); }
    @PostMapping("/diagnosticos") public Diagnostico diagnostico(@AuthenticationPrincipal Usuario u,@RequestParam String mes) { return ia.diagnosticar(u.id,mes); }
}
