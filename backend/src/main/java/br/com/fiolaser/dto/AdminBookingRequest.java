package br.com.fiolaser.dto;

import br.com.fiolaser.model.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record AdminBookingRequest(
    @NotBlank String userId,
    @NotBlank String serviceId,
    @NotNull LocalDateTime horario,
    @NotNull PaymentMethod formaPagamento) {
  public BookingRequest booking() {
    return new BookingRequest(serviceId, horario, formaPagamento);
  }
}
