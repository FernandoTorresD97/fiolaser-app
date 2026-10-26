package br.com.fiolaser.dto;

import br.com.fiolaser.model.BookingStatus;
import br.com.fiolaser.model.PaymentMethod;
import java.time.LocalDateTime;

public record BookingResponse(
    String id,
    String servicoNome,
    LocalDateTime horario,
    PaymentMethod formaPagamento,
    BookingStatus status) {}
