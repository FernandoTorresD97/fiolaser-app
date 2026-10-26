package br.com.fiolaser.dto;

import br.com.fiolaser.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminBookingResponse(
    String id,
    String clienteNome,
    String clienteEmail,
    String clienteTelefone,
    String servicoNome,
    LocalDateTime horario,
    PaymentMethod formaPagamento,
    BookingStatus status,
    String userId,
    String serviceId,
    LocalDateTime fimHorario,
    BigDecimal valor) {}
