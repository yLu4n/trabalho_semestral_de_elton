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
        this.sender=sender; this.modo=modo; this.remetente=remetente; this.url=url;
    }
    public void enviarVerificacao(String email,String token) {
        String texto="Confirme seu e-mail no FinAI. O link expira em 24 horas.\n"+url+"/verificar?token="+token;
        try {
            if("file".equals(modo)) {
                Path pasta=Path.of("var","mail"); Files.createDirectories(pasta);
                Files.writeString(pasta.resolve(UUID.randomUUID()+".eml"),"To: "+email+"\nSubject: Confirme seu e-mail no FinAI\nContent-Type: text/plain; charset=UTF-8\n\n"+texto);
            } else {
                SimpleMailMessage mensagem=new SimpleMailMessage(); mensagem.setFrom(remetente); mensagem.setTo(email);
                mensagem.setSubject("Confirme seu e-mail no FinAI"); mensagem.setText(texto); sender.send(mensagem);
            }
        } catch(Exception e) { throw new RegraNegocioException(503,"Não foi possível enviar o e-mail de verificação. Tente novamente."); }
    }
}
