package br.com.finai.config;

import br.com.finai.service.AutenticacaoService;
import br.com.finai.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.*;
import java.io.IOException;

@Configuration
public class SegurancaConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain seguranca(HttpSecurity http,AutenticacaoService auth) throws Exception {
        var csrfRepo=CookieCsrfTokenRepository.withHttpOnlyFalse();
        var handler=new CsrfTokenRequestAttributeHandler();
        http.sessionManagement(s->s.disable())
            .securityContext(c->c.requireExplicitSave(true).securityContextRepository(new org.springframework.security.web.context.NullSecurityContextRepository()))
            .requestCache(c->c.disable())
            .csrf(c->c.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(handler))
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/csrf","/api/auth/cadastro","/api/auth/login","/api/auth/verificar","/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/dpo/**").hasRole("DPO")
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/**").hasRole("TITULAR")
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->erro(res,401,"Sua sessão expirou. Entre novamente."))
                .accessDeniedHandler((req,res,ex)->erro(res,403,"Acesso negado ou token de proteção inválido.")))
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
                    String token=cookie(req,"FINAI_SESSION");
                    if(token!=null) auth.autenticar(token).ifPresent(u->SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(u,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.papel.name())))));
                    chain.doFilter(req,res);
                }
            },UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    public static String cookie(HttpServletRequest req,String nome) {
        if(req.getCookies()==null) return null;
        return Arrays.stream(req.getCookies()).filter(c->c.getName().equals(nome)).map(Cookie::getValue).findFirst().orElse(null);
    }
    private static void erro(HttpServletResponse res,int status,String mensagem) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"mensagem\":\""+mensagem+"\"}");
    }
}
