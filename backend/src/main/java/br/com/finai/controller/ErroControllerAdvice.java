package br.com.finai.controller;

import br.com.finai.service.RegraNegocioException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.dao.*;
import java.util.Map;

@RestControllerAdvice
public class ErroControllerAdvice {
    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<?> regra(RegraNegocioException e) {
        return ResponseEntity.status(e.status).body(Map.of("mensagem", e.getMessage()));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class, java.time.DateTimeException.class })
    ResponseEntity<?> validacao(Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", "Confira os campos informados e os formatos de data e valor."));
    }

    @ExceptionHandler({ DataIntegrityViolationException.class, OptimisticLockingFailureException.class })
    ResponseEntity<?> conflito(Exception e) {
        return ResponseEntity.status(409)
                .body(Map.of("mensagem", "Este registro já existe ou foi alterado. Atualize a página."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> inesperado(Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha interna de tipo {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError()
                .body(Map.of("mensagem", "Não foi possível concluir a operação. Tente novamente."));
    }
}
