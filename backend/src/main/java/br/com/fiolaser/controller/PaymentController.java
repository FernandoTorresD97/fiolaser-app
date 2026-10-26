package br.com.fiolaser.controller;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  @GetMapping("/capabilities")
  public Map<String, Object> capabilities() {
    return Map.of(
        "online",
        false,
        "mensagem",
        "Pagamento online ainda não configurado. O agendamento registra a forma de pagamento para atendimento na unidade.");
  }

  @PostMapping("/checkout")
  public ResponseEntity<Map<String, String>> checkout() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("erro", "Cobrança online não configurada. Nenhuma cobrança foi realizada."));
  }
}
