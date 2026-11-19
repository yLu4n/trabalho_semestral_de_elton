package br.com.finai.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Profile("production")
public class ProducaoConfig {
    public ProducaoConfig(@Value("${finai.cookie-secure}") boolean seguro, @Value("${finai.public-url}") String url,
            @Value("${finai.mail-mode}") String email) {
        if (!seguro || !url.startsWith("https://") || !email.equals("smtp"))
            throw new IllegalStateException("Produção exige COOKIE_SECURE=true, PUBLIC_URL=https e MAIL_MODE=smtp.");
    }
}
