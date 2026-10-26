package br.com.fiolaser.dto;

import br.com.fiolaser.model.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record BookingRequest(
    @NotBlank String serviceId,
    @NotNull LocalDateTime horario,
    @NotNull PaymentMethod formaPagamento) {}
