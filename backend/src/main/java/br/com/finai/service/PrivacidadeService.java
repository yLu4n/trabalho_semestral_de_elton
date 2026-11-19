package br.com.finai.service;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
@Service
public class PrivacidadeService {
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final LancamentoRepository lancamentos;
    private final SugestaoLancamentoRepository sugestoes;
    private final SolicitacaoPrivacidadeRepository solicitacoes;
    private final EventoAuditoriaRepository auditoria;
    private final AutenticacaoService auth;
    private final EntityManager em;
    public PrivacidadeService(UsuarioRepository usuarios,SessaoRepository sessoes,LancamentoRepository lancamentos,
        SugestaoLancamentoRepository sugestoes,SolicitacaoPrivacidadeRepository solicitacoes,EventoAuditoriaRepository auditoria,AutenticacaoService auth,EntityManager em) {
        this.usuarios=usuarios; this.sessoes=sessoes; this.lancamentos=lancamentos; this.sugestoes=sugestoes; this.solicitacoes=solicitacoes; this.auditoria=auditoria; this.auth=auth; this.em=em;
    }
    @Transactional public SolicitacaoPrivacidade solicitarExclusao(Usuario usuario,String senha) {
        auth.reautenticar(usuario,senha);
        Usuario u=usuarios.bloquearParaAtualizacao(usuario.id).orElseThrow(); u.exclusaoSolicitadaEm=Instant.now(); usuarios.save(u);
        sessoes.deleteByUsuarioId(u.id);
        SolicitacaoPrivacidade s=new SolicitacaoPrivacidade(); s.usuarioId=u.id; s.tipo="EXCLUSAO_CONTA"; solicitacoes.save(s);
        evento("EXCLUSAO_SOLICITADA"); return s;
    }
    @Transactional public SolicitacaoPrivacidade solicitar(UUID usuario,String tipo) {
        if(!Set.of("ACESSO","CORRECAO","INFORMACOES").contains(tipo)) throw new RegraNegocioException("Tipo de solicitação inválido.");
        SolicitacaoPrivacidade s=new SolicitacaoPrivacidade(); s.usuarioId=usuario; s.tipo=tipo; return solicitacoes.save(s);
    }
    public List<SolicitacaoPrivacidade> minhas(UUID usuario) { return solicitacoes.findByUsuarioId(usuario); }
    @Scheduled(fixedDelay=3600000) @Transactional public void aplicarRetencao() {
        sugestoes.deleteByCriadaEmBefore(Instant.now().minusSeconds(1800));
        lancamentos.deleteAll(lancamentos.findByExcluidoEmBefore(Instant.now().minusSeconds(90L*86400)));
        em.createQuery("delete from Sessao s where s.ultimaAtividade < :limite").setParameter("limite",Instant.now().minusSeconds(1800)).executeUpdate();
        em.createQuery("delete from VerificacaoEmail v where v.expiraEm < :agora").setParameter("agora",Instant.now()).executeUpdate();
        // Elimina em até 24h, dentro do prazo máximo de 15 dias do documento.
        for(Usuario u:usuarios.findByExclusaoSolicitadaEmBefore(Instant.now().minusSeconds(86400))) {
            em.createQuery("delete from Lancamento l where l.usuarioId = :id").setParameter("id",u.id).executeUpdate();
            em.createQuery("delete from MetaOrcamento m where m.usuarioId = :id").setParameter("id",u.id).executeUpdate();
            em.createQuery("delete from CorrecaoCategoria c where c.usuarioId = :id").setParameter("id",u.id).executeUpdate();
            for(var s:solicitacoes.findByUsuarioId(u.id)) { s.estado="CONCLUIDA"; s.usuarioId=null; solicitacoes.save(s); }
            usuarios.delete(u); usuarios.flush(); evento("CONTA_ELIMINADA");
        }
    }
    public void evento(String acao) { EventoAuditoria e=new EventoAuditoria(); e.acao=acao; auditoria.save(e); }
}
