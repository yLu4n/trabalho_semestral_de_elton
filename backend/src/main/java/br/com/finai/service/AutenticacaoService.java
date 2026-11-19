package br.com.finai.service;

import br.com.finai.dto.Contratos.*;
import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final VerificacaoEmailRepository verificacoes;
    private final PasswordEncoder senhas;
    private final EmailService email;
    private final EventoAuditoriaRepository auditoria;
    private final byte[] segredo;
    private final String hashFalso;
    private final java.util.concurrent.ConcurrentMap<UUID, Instant> atividadesRecentes = new java.util.concurrent.ConcurrentHashMap<>();

    public AutenticacaoService(UsuarioRepository usuarios, SessaoRepository sessoes,
            VerificacaoEmailRepository verificacoes,
            PasswordEncoder senhas, EmailService email, EventoAuditoriaRepository auditoria, @Value("${finai.secret}") String segredo) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.verificacoes = verificacoes;
        this.senhas = senhas;
        this.email = email;
        this.auditoria = auditoria;
        if (segredo.length() < 32)
            throw new IllegalStateException("SESSION_SECRET precisa de pelo menos 32 caracteres.");
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.hashFalso = senhas.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public void cadastrar(Cadastro dados) {
        if (dados.nascimento().isAfter(RegrasFinanceiras.hoje().minusYears(18)))
            throw new RegraNegocioException("É necessário ter pelo menos 18 anos.");
        validarSenha(dados.senha());
        String endereco = dados.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmail(endereco).isPresent())
            throw new RegraNegocioException(409, "Este e-mail já está cadastrado.");
        Usuario u = new Usuario();
        u.nome = dados.nome().trim();
        u.email = endereco;
        u.nascimento = dados.nascimento();
        u.senhaHash = senhas.encode(dados.senha());
        try {
            usuarios.saveAndFlush(u);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RegraNegocioException(409, "Este e-mail já está cadastrado.");
        }
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        VerificacaoEmail v = new VerificacaoEmail();
        v.usuarioId = u.id;
        v.tokenHash = hash(token);
        verificacoes.save(v);
        email.enviarVerificacao(u.email, token);
    }

    public static void validarSenha(String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) < 8
                || senha.getBytes(StandardCharsets.UTF_8).length > 72
                || senha.codePoints().noneMatch(Character::isLetter) || senha.codePoints().noneMatch(Character::isDigit))
            throw new RegraNegocioException("Use ao menos oito caracteres, incluindo uma letra e um número, até 72 bytes.");
    }

    @Transactional
    public void verificar(String token) {
        Usuario u = usuarioDoVinculo(token, false);
        VerificacaoEmail v = vinculo(token, false);
        u.emailVerificado = true;
        usuarios.save(u);
        verificacoes.delete(v);
    }

    @Transactional
    public void solicitarRecuperacao(RecuperacaoSenha dados) {
        var conta = usuarios.findByEmail(dados.email().trim().toLowerCase(Locale.ROOT));
        if (conta.isEmpty())
            return;
        Usuario u = usuarios.bloquearParaAtualizacao(conta.get().id).orElse(null);
        if (u == null || u.bloqueado || u.exclusaoSolicitadaEm != null)
            return;
        verificacoes.deleteByUsuarioIdAndRecuperacaoTrue(u.id);
        verificacoes.flush();
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        VerificacaoEmail v = new VerificacaoEmail();
        v.usuarioId = u.id;
        v.tokenHash = hash(token);
        v.recuperacao = true;
        v.expiraEm = Instant.now().plusSeconds(1800);
        verificacoes.save(v);
        email.enviarRecuperacao(u.email, token);
    }

    @Transactional
    public void redefinirSenha(RedefinicaoSenha dados) {
        validarSenha(dados.senha());
        Usuario u = usuarioDoVinculo(dados.token(), true);
        VerificacaoEmail v = vinculo(dados.token(), true);
        u.senhaHash = senhas.encode(dados.senha());
        u.tentativas = 0;
        u.bloqueadoAte = null;
        usuarios.save(u);
        sessoes.deleteByUsuarioId(u.id);
        verificacoes.delete(v);
    }

    private Usuario usuarioDoVinculo(String token, boolean recuperacao) {
        VerificacaoEmail v = vinculo(token, recuperacao);
        return usuarios.bloquearParaAtualizacao(v.usuarioId)
                .filter(u -> u.exclusaoSolicitadaEm == null && (!recuperacao || !u.bloqueado))
                .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado."));
    }

    private VerificacaoEmail vinculo(String token, boolean recuperacao) {
        return verificacoes.findByTokenHash(hash(token))
                .filter(v -> v.recuperacao == recuperacao && v.expiraEm.isAfter(Instant.now()))
                .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado."));
    }

    @Transactional(noRollbackFor = RegraNegocioException.class)
    public String entrar(Login dados) {
        Usuario encontrado = usuarios.findByEmail(dados.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        Usuario u = encontrado == null ? null : usuarios.bloquearParaAtualizacao(encontrado.id).orElse(null);
        boolean correta = senhas.matches(dados.senha(), u == null ? hashFalso : u.senhaHash);
        if (u == null)
            throw new RegraNegocioException(401, "E-mail ou senha inválidos.");
        if (u.bloqueado || u.exclusaoSolicitadaEm != null
                || (u.bloqueadoAte != null && u.bloqueadoAte.isAfter(Instant.now())))
            throw new RegraNegocioException(401, "Conta indisponível ou temporariamente bloqueada.");
        if (!correta) {
            u.tentativas++;
            if (u.tentativas >= 5) {
                u.bloqueadoAte = Instant.now().plusSeconds(900);
                u.tentativas = 0;
            }
            usuarios.save(u);
            throw new RegraNegocioException(401, "E-mail ou senha inválidos.");
        }
        if (u.papel != Papel.TITULAR && !u.emailVerificado)
            throw new RegraNegocioException(403, "Confirme seu e-mail antes de entrar.");
        u.tentativas = 0;
        u.bloqueadoAte = null;
        usuarios.save(u);
        Sessao s = new Sessao();
        s.usuarioId = u.id;
        sessoes.save(s);
        auditoria.save(EventoAuditoria.de("LOGIN_CONCLUIDO", s.id));
        String payload = s.id + "." + s.criadaEm.plusSeconds(43200).getEpochSecond();
        return payload + "." + assinar(payload);
    }

    public Optional<Usuario> autenticar(String token) {
        try {
            String[] p = token.split("\\.");
            if (p.length != 3
                    || !MessageDigest.isEqual(assinar(p[0] + "." + p[1]).getBytes(StandardCharsets.UTF_8),
                            p[2].getBytes(StandardCharsets.UTF_8))
                    || Long.parseLong(p[1]) <= Instant.now().getEpochSecond())
                return Optional.empty();
            Sessao s = sessoes.findById(UUID.fromString(p[0])).orElse(null);
            Instant agora = Instant.now();
            if (s == null)
                return Optional.empty();
            Instant ultima = atividadesRecentes.getOrDefault(s.id, s.ultimaAtividade);
            if (s.ultimaAtividade.isAfter(ultima))
                ultima = s.ultimaAtividade;
            if (ultima.plusSeconds(1800).isBefore(agora) || s.criadaEm.plusSeconds(43200).isBefore(agora)) {
                atividadesRecentes.remove(s.id);
                return Optional.empty();
            }
            Usuario u = usuarios.findById(s.usuarioId).orElse(null);
            if (u == null || u.bloqueado || u.exclusaoSolicitadaEm != null
                    || (u.papel != Papel.TITULAR && !u.emailVerificado))
                return Optional.empty();
            atividadesRecentes.merge(s.id, agora, (a, b) -> a.isAfter(b) ? a : b);
            // Agrupa somente metadados de atividade; autorização e propriedade continuam
            // verificadas a cada chamada.
            if (s.ultimaAtividade.isBefore(agora.minusSeconds(5)))
                sessoes.atualizarAtividade(s.id, agora, agora.minusSeconds(5));
            return Optional.of(u);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void sair(String token) {
        try {
            UUID id = UUID.fromString(token.split("\\.")[0]);
            atividadesRecentes.remove(id);
            sessoes.deleteById(id);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void limparAtividadesExpiradas() {
        Instant limite = Instant.now().minusSeconds(1800);
        atividadesRecentes.entrySet().removeIf(e -> e.getValue().isBefore(limite));
    }

    public void reautenticar(Usuario usuario, String senha) {
        if (!senhas.matches(senha, usuario.senhaHash))
            throw new RegraNegocioException(401, "Senha incorreta.");
    }

    private String assinar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hash(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
