package br.com.finai.service;

import br.com.finai.dto.Contratos.Login;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtividadeSessaoTest {
    @Test
    void atividadeRecentePreservaSessaoSemGravarACadaConsultaEBloqueioContinuaImediato() {
        var usuarios = mock(UsuarioRepository.class);
        var sessoes = mock(SessaoRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        var usuario = new Usuario();
        usuario.email = "atividade@finai.example";
        usuario.emailVerificado = true;
        usuario.senhaHash = encoder.encode("TesteSeguro123!");
        when(usuarios.findByEmail(usuario.email)).thenReturn(Optional.of(usuario));
        when(usuarios.findById(usuario.id)).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaAtualizacao(usuario.id)).thenReturn(Optional.of(usuario));
        when(sessoes.save(any())).thenAnswer(i -> {
            Sessao s = i.getArgument(0);
            when(sessoes.findById(s.id)).thenReturn(Optional.of(s));
            return s;
        });
        var service = new AutenticacaoService(usuarios, sessoes, mock(VerificacaoEmailRepository.class), encoder,
                mock(EmailService.class), mock(EventoAuditoriaRepository.class), "segredo_de_teste_com_pelo_menos_32_caracteres");
        String token = service.entrar(new Login(usuario.email, "TesteSeguro123!"));
        UUID id = UUID.fromString(token.split("\\.")[0]);
        Sessao sessao = sessoes.findById(id).orElseThrow();
        sessao.ultimaAtividade = Instant.now().minusSeconds(10);
        assertThat(service.autenticar(token)).contains(usuario);
        verify(sessoes).atualizarAtividade(eq(id), any(), any());
        sessao.ultimaAtividade = Instant.now();
        for (int i = 0; i < 10; i++)
            assertThat(service.autenticar(token)).contains(usuario);
        verify(sessoes, times(1)).atualizarAtividade(eq(id), any(), any());
        // Um relógio persistido antigo não encerra uma sessão comprovadamente ativa
        // nesta instância.
        sessao.ultimaAtividade = Instant.now().minusSeconds(1801);
        assertThat(service.autenticar(token)).contains(usuario);
        usuario.bloqueado = true;
        assertThat(service.autenticar(token)).isEmpty();
        service.limparAtividadesExpiradas();
    }
}
