package br.com.fiolaser.kafka;

import br.com.fiolaser.kafka.events.BookingCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingEventProducer {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public BookingEventProducer(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public void publicarAgendamentoCriado(BookingCreatedEvent event) {
    try {
      jdbc.update(
          "INSERT INTO booking_outbox(id,payload) VALUES (?,?)",
          event.eventId(),
          json.writeValueAsString(event));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Falha ao registrar confirmação.", e);
    }
  }
}
