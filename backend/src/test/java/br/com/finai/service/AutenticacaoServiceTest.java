package br.com.finai.service;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AutenticacaoServiceTest {
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    SessaoRepository sessoes = mock(SessaoRepository.class);
    VerificacaoEmailRepository verificacoes = mock(VerificacaoEmailRepository.class);
    EmailService email = mock(EmailService.class);
    EventoAuditoriaRepository auditoria = mock(EventoAuditoriaRepository.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AutenticacaoService service = new AutenticacaoService(usuarios, sessoes, verificacoes, encoder, email, auditoria,
            "segredo_de_teste_com_mais_de_32_caracteres");
    Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.email = "teste@exemplo.com";
        usuario.senhaHash = encoder.encode("SeguraSenha123!");
        usuario.emailVerificado = true;
        when(usuarios.findByEmail(usuario.email)).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearParaAtualizacao(usuario.id)).thenReturn(Optional.of(usuario));
        when(usuarios.findById(usuario.id)).thenReturn(Optional.of(usuario));
        when(sessoes.save(any())).thenAnswer(i -> {
            Sessao s = i.getArgument(0);
            when(sessoes.findById(s.id)).thenReturn(Optional.of(s));
            return s;
        });
    }

    @Test
    void loginTokenAssinadoLogout() {
        String token = service.entrar(new Login(usuario.email, "SeguraSenha123!"));
        assertThat(service.autenticar(token)).contains(usuario);
        assertThat(service.autenticar(token + "x")).isEmpty();
        assertThat(service.autenticar("invalido")).isEmpty();
        service.sair(token);
        UUID idSessao = UUID.fromString(token.split("\\.")[0]);
        verify(sessoes).deleteById(idSessao);
        verify(auditoria).save(argThat(e -> "LOGIN_CONCLUIDO".equals(e.acao) && idSessao.equals(e.correlacaoId)));
        service.reautenticar(usuario, "SeguraSenha123!");
        assertThatThrownBy(() -> service.reautenticar(usuario, "errada")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void bloqueiaAposCincoFalhas() {
        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> service.entrar(new Login(usuario.email, "errada")))
                    .isInstanceOf(RegraNegocioException.class);
        assertThat(usuario.bloqueadoAte).isAfter(Instant.now());
        assertThatThrownBy(() -> service.entrar(new Login(usuario.email, "SeguraSenha123!")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void expiraPorInatividadeEDuracao() {
        String token = service.entrar(new Login(usuario.email, "SeguraSenha123!"));
        UUID id = UUID.fromString(token.split("\\.")[0]);
        Sessao s = sessoes.findById(id).orElseThrow();
        s.ultimaAtividade = Instant.now().minusSeconds(1801);
        assertThat(service.autenticar(token)).isEmpty();
        s.ultimaAtividade = Instant.now();
        s.criadaEm = Instant.now().minusSeconds(43201);
        assertThat(service.autenticar(token)).isEmpty();
    }

    @Test
    void permiteTitularPendenteENegaContaBloqueada() {
        usuario.emailVerificado = false;
        String token = service.entrar(new Login(usuario.email, "SeguraSenha123!"));
        assertThat(service.autenticar(token)).contains(usuario);
        usuario.emailVerificado = true;
        usuario.bloqueado = true;
        assertThatThrownBy(() -> service.entrar(new Login(usuario.email, "SeguraSenha123!")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.entrar(new Login("ausente@exemplo.com", "invalida")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void recuperaSenhaComPrazoRevogaSessoesEImpedeReuso() {
        var enviado = org.mockito.ArgumentCaptor.forClass(VerificacaoEmail.class);
        var token = org.mockito.ArgumentCaptor.forClass(String.class);
        service.solicitarRecuperacao(new RecuperacaoSenha(usuario.email));
        verify(verificacoes).save(enviado.capture());
        verify(email).enviarRecuperacao(eq(usuario.email), token.capture());
        var registro = enviado.getValue();
        assertThat(registro.tokenHash).isEqualTo(AutenticacaoService.hash(token.getValue()));
        assertThat(registro.recuperacao).isTrue();
        assertThat(registro.expiraEm).isBetween(Instant.now().plusSeconds(1790), Instant.now().plusSeconds(1800));
        when(verificacoes.findByTokenHash(registro.tokenHash)).thenReturn(Optional.of(registro));
        registro.expiraEm = Instant.now().minusSeconds(1);
        var redefinicao = new RedefinicaoSenha(token.getValue(), "abc12345");
        assertThatThrownBy(() -> service.redefinirSenha(redefinicao)).isInstanceOf(RegraNegocioException.class);
        registro.expiraEm = Instant.now().plusSeconds(60);
        assertThatThrownBy(() -> service.verificar(token.getValue())).isInstanceOf(RegraNegocioException.class);
        service.redefinirSenha(redefinicao);
        assertThat(encoder.matches("abc12345", usuario.senhaHash)).isTrue();
        verify(sessoes).deleteByUsuarioId(usuario.id);
        verify(verificacoes).delete(registro);
        when(verificacoes.findByTokenHash(registro.tokenHash)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.redefinirSenha(redefinicao)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cadastraAdultoVerificaEmailERejeitaMenor() {
        var dados = new Cadastro("Teste", "novo@exemplo.com", "SeguraSenha123!", LocalDate.of(2000, 1, 1), true);
        service.cadastrar(dados);
        verify(email).enviarVerificacao(eq(dados.email()), anyString());
        assertThatThrownBy(() -> service.cadastrar(new Cadastro("Menor", "menor@exemplo.com", "SeguraSenha123!",
                RegrasFinanceiras.hoje().minusYears(17), true))).isInstanceOf(RegraNegocioException.class);
        VerificacaoEmail v = new VerificacaoEmail();
        v.usuarioId = usuario.id;
        when(verificacoes.findByTokenHash(AutenticacaoService.hash("token"))).thenReturn(Optional.of(v));
        service.verificar("token");
        verify(verificacoes).delete(v);
        v.expiraEm = Instant.now().minusSeconds(1);
        assertThatThrownBy(() -> service.verificar("token")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.verificar("ausente")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.cadastrar(new Cadastro("Existente", usuario.email, "abc12345",
                LocalDate.of(1990, 1, 1), true))).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está cadastrado");
    }
}
