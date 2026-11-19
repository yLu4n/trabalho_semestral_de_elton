package br.com.finai.service;

import br.com.finai.repository.*;
import br.com.finai.model.UsoIa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Service
public class CotaIaService {
    private final UsuarioRepository usuarios;
    private final UsoIaRepository usos;

    public CotaIaService(UsuarioRepository usuarios, UsoIaRepository usos) {
        this.usuarios = usuarios;
        this.usos = usos;
    }

    @Transactional
    public void reservar(UUID usuario) {
        usuarios.bloquearParaAtualizacao(usuario).orElseThrow(() -> new RegraNegocioException(401, "Conta inválida."));
        LocalDate hoje = RegrasFinanceiras.hoje();
        YearMonth mes = YearMonth.from(hoje);
        int total = usos.findByUsuarioIdAndDiaBetween(usuario, mes.atDay(1), mes.atEndOfMonth()).stream()
                .mapToInt(u -> u.quantidade).sum();
        UsoIa uso = usos.findByUsuarioIdAndDia(usuario, hoje).orElseGet(UsoIa::new);
        if (uso.quantidade >= 60 || total >= 400)
            throw new RegraNegocioException(429, "Limite da IA atingido. O registro manual continua disponível.");
        uso.usuarioId = usuario;
        uso.dia = hoje;
        uso.quantidade++;
        usos.save(uso);
    }
}
