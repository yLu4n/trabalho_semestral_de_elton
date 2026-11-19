package br.com.finai.service;

import br.com.finai.model.*;
import br.com.finai.repository.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PrivacidadeServiceTest {
    @Test
    void solicitaRevogaSessoesEAplicaRetencao() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        SessaoRepository sessoes = mock(SessaoRepository.class);
        LancamentoRepository lancamentos = mock(LancamentoRepository.class);
        SugestaoLancamentoRepository sugestoes = mock(SugestaoLancamentoRepository.class);
        SolicitacaoPrivacidadeRepository solicitacoes = mock(SolicitacaoPrivacidadeRepository.class);
        EventoAuditoriaRepository auditoria = mock(EventoAuditoriaRepository.class);
        AutenticacaoService auth = mock(AutenticacaoService.class);
        EntityManager em = mock(EntityManager.class);
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var email = mock(EmailService.class);
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        Usuario u = new Usuario();
        u.email = "titular@example.test";
        when(usuarios.bloquearParaAtualizacao(u.id)).thenReturn(Optional.of(u));
        when(solicitacoes.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new PrivacidadeService(usuarios, sessoes, lancamentos, sugestoes, solicitacoes, auditoria, auth,
                em, jdbc, email);
        assertThat(service.solicitarExclusao(u, "senha").tipo).isEqualTo("EXCLUSAO_CONTA");
        verify(auth).reautenticar(u, "senha");
        verify(sessoes).deleteByUsuarioId(u.id);
        assertThat(u.exclusaoSolicitadaEm).isBeforeOrEqualTo(Instant.now());
        assertThat(service.solicitar(u.id, "ACESSO").estado).isEqualTo("ABERTA");
        assertThatThrownBy(() -> service.solicitar(u.id, "INVALIDO")).isInstanceOf(RegraNegocioException.class);
        when(usuarios.findByExclusaoSolicitadaEmBefore(any())).thenReturn(List.of(u));
        SolicitacaoPrivacidade s = new SolicitacaoPrivacidade();
        s.usuarioId = u.id;
        when(solicitacoes.findByUsuarioId(u.id)).thenReturn(List.of(s));
        assertThat(service.minhas(u.id)).hasSize(1);
        service.aplicarRetencao();
        verify(usuarios).delete(u);
        verify(jdbc).update(startsWith("insert into aviso_exclusao"), any(UUID.class), eq(u.email),
                any(java.sql.Timestamp.class));
        verifyNoInteractions(email);
        assertThat(s.usuarioId).isNull();
        assertThat(s.estado).isEqualTo("CONCLUIDA");
        verify(sugestoes).deleteByCriadaEmBefore(any());
        verify(auditoria, times(2)).save(any());
    }
}
