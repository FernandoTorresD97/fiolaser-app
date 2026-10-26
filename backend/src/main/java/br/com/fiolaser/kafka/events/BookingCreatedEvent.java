package br.com.fiolaser.kafka.events;

import java.time.LocalDateTime;

public record BookingCreatedEvent(
    String eventId,
    String bookingId,
    String userId,
    String userNome,
    String userEmail,
    String userTelefone,
    String servicoNome,
    LocalDateTime horario,
    String formaPagamento) {}
