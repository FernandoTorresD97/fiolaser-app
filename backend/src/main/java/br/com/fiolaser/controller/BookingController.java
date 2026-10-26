package br.com.fiolaser.controller;

import br.com.fiolaser.dto.BookingRequest;
import br.com.fiolaser.dto.BookingResponse;
import br.com.fiolaser.dto.SlotResponse;
import br.com.fiolaser.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  // rota publica: precisa ser vista antes do login pra montar o calendario na tela de agendamento
  @GetMapping("/availability/{serviceId}")
  public List<SlotResponse> disponibilidade(
      @PathVariable String serviceId,
      @RequestParam("dia")
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate dia) {
    return bookingService.disponibilidadeDoDia(serviceId, dia);
  }

  @PostMapping
  public ResponseEntity<BookingResponse> criar(
      Authentication auth, @Valid @RequestBody BookingRequest req) {
    return ResponseEntity.ok(bookingService.criar(auth.getName(), req));
  }

  @GetMapping("/me")
  public List<BookingResponse> meusAgendamentos(Authentication auth) {
    return bookingService.meusAgendamentos(auth.getName());
  }

  @DeleteMapping("/{bookingId}")
  public ResponseEntity<Void> cancelar(Authentication auth, @PathVariable String bookingId) {
    bookingService.cancelar(auth.getName(), bookingId);
    return ResponseEntity.noContent().build();
  }
}
