package br.com.fiolaser.controller;

import br.com.fiolaser.dto.*;
import br.com.fiolaser.service.BookingService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {
  private final BookingService service;

  public AdminBookingController(BookingService service) {
    this.service = service;
  }

  @GetMapping
  public List<AdminBookingResponse> listar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate inicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dia) {
    return service.listarParaAdmin(dia != null ? dia : inicio, dia != null ? dia : fim);
  }

  @PostMapping
  public AdminBookingResponse criar(@Valid @RequestBody AdminBookingRequest req) {
    return service.criarComoAdmin(req);
  }

  @PutMapping("/{id}")
  public AdminBookingResponse editar(
      @PathVariable String id, @Valid @RequestBody AdminBookingRequest req) {
    return service.editarComoAdmin(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancelar(@PathVariable String id) {
    service.cancelarComoAdmin(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
    StringBuilder csv =
        new StringBuilder(
            "\uFEFFCliente;Email;Telefone;Procedimento;Inicio;Fim;Forma de pagamento;Status;Valor contratado\r\n");
    for (var b : service.listarParaAdmin(inicio, fim)) {
      csv.append(
              String.join(
                  ";",
                  cell(b.clienteNome()),
                  cell(b.clienteEmail()),
                  cell(b.clienteTelefone()),
                  cell(b.servicoNome()),
                  cell(b.horario()),
                  cell(b.fimHorario()),
                  cell(b.formaPagamento()),
                  cell(b.status()),
                  cell(b.valor())))
          .append("\r\n");
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=agendamentos.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  public static String cell(Object value) {
    String s = value == null ? "" : value.toString();
    if ((!s.stripLeading().isEmpty() && "=+@-".indexOf(s.stripLeading().charAt(0)) >= 0)
        || s.startsWith("\t")
        || s.startsWith("\r")
        || s.startsWith("\n")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
