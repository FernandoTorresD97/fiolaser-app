package br.com.fiolaser.controller;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(br.com.fiolaser.security.AuthThrottledException.class)
  public ResponseEntity<?> throttled(br.com.fiolaser.security.AuthThrottledException e) {
    return ResponseEntity.status(429).header("Retry-After",Long.toString(e.retryAfter())).body(Map.of("erro",e.getMessage()));
  }
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> argument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> state(IllegalStateException e) {
    return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "erro",
                "Dados já cadastrados ou horário indisponível. Atualize e tente novamente."));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<?> denied() {
    return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(errors);
  }
}
