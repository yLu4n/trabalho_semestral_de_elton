package br.com.finai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.util.UUID;

@Service
public class EmailService {
    private final JavaMailSender sender;
    private final String modo, remetente, url;

    public EmailService(JavaMailSender sender, @Value("${finai.mail-mode}") String modo,
            @Value("${finai.mail-from}") String remetente, @Value("${finai.public-url}") String url) {
        this.sender = sender;
        this.modo = modo;
        this.remetente = remetente;
        this.url = url;
    }

    public void enviarVerificacao(String email, String token) {
        enviar(email, "Confirme seu e-mail no FinAI",
                "Confirme seu e-mail no FinAI. O link expira em 24 horas.\n" + url + "/verificar?token=" + token);
    }

    public void enviarRecuperacao(String email, String token) {
        enviar(email, "Redefinição de senha no FinAI",
                "Este link pode ser usado uma vez e expira em 30 minutos.\n" + url + "/redefinir?token=" + token);
    }

    public void enviarConclusaoExclusao(String email) {
        enviar(email, "Exclusão de conta concluída no FinAI", "A exclusão da sua conta FinAI foi concluída.");
    }

    private void enviar(String email, String assunto, String texto) {
        try {
            if ("file".equals(modo)) {
                Path pasta = Path.of("var", "mail");
                Files.createDirectories(pasta);
                Files.writeString(pasta.resolve(UUID.randomUUID() + ".eml"), "To: " + email + "\nSubject: " + assunto
                        + "\nContent-Type: text/plain; charset=UTF-8\n\n" + texto);
            } else {
                SimpleMailMessage mensagem = new SimpleMailMessage();
                mensagem.setFrom(remetente);
                mensagem.setTo(email);
                mensagem.setSubject(assunto);
                mensagem.setText(texto);
                sender.send(mensagem);
            }
        } catch (Exception e) {
            throw new RegraNegocioException(503, "Não foi possível enviar o e-mail. Tente novamente.");
        }
    }
}
