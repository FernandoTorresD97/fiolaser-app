package br.com.fiolaser.notification;

import br.com.fiolaser.kafka.events.BookingCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class NotificationConsumer {
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;
  private final JavaMailSender mail;
  private final TransactionTemplate tx;
  private final String from;
  private final org.springframework.beans.factory.ObjectProvider<SmsSender> sms;

  public NotificationConsumer(
      ObjectMapper json,
      JdbcTemplate jdbc,
      JavaMailSender mail,
      PlatformTransactionManager manager,
      @Value("${app.notifications.from}") String from,
      org.springframework.beans.factory.ObjectProvider<SmsSender> sms) {
    this.json = json;
    this.jdbc = jdbc;
    this.mail = mail;
    this.tx = new TransactionTemplate(manager);
    this.from = from;
    this.sms = sms;
  }

  @KafkaListener(topics = "booking-created", groupId = "notification-service")
  public void receive(String payload) throws Exception {
    var event = json.readValue(payload, BookingCreatedEvent.class);
    if (event.eventId() == null || event.bookingId() == null)
      throw new IllegalArgumentException("Evento sem identificador.");
    boolean useSms = event.userEmail() == null || event.userEmail().isBlank();
    String channel = useSms ? "SMS" : "EMAIL";
    tx.executeWithoutResult(
        status -> {
          // Serializes duplicate deliveries between worker replicas. SMTP itself is at-least-once.
          jdbc.queryForObject(
              "SELECT pg_advisory_xact_lock(hashtext(?))", Object.class, event.eventId());
          if (Boolean.TRUE.equals(
              jdbc.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM notification_deliveries WHERE event_id=? AND channel=?)",
                  Boolean.class,
                  event.eventId(),
                  channel))) return;
          boolean current =
              Boolean.TRUE.equals(
                  jdbc.queryForObject(
                      "SELECT EXISTS(SELECT 1 FROM bookings WHERE id=? AND horario=? AND status IN ('RESERVADO','CONFIRMADO'))",
                      Boolean.class,
                      event.bookingId(),
                      java.sql.Timestamp.valueOf(event.horario())));
          if (!current) return;
          if (useSms) {
            SmsSender sender = sms.getIfAvailable();
            if (sender == null)
              throw new IllegalStateException("SMS não configurado para conta sem e-mail.");
            sender.send(
                event.userTelefone(),
                "Fiolaser: reserva recebida para "
                    + event.horario().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " (Bahia). Não comprova pagamento.");
          } else {
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.userEmail());
            message.setSubject("Fiolaser — reserva recebida");
            message.setText(
                "Olá, "
                    + event.userNome()
                    + ".\n\nSua reserva de "
                    + event.servicoNome()
                    + " foi recebida para "
                    + event.horario().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))
                    + " (horário da Bahia).\n"
                    + "Forma de pagamento escolhida: "
                    + event.formaPagamento()
                    + ". Esta mensagem não comprova pagamento.\n"
                    + "Código da reserva: "
                    + event.bookingId()
                    + ".");
            mail.send(message);
          }
          jdbc.update(
              "INSERT INTO notification_deliveries(event_id,channel) VALUES (?,?)",
              event.eventId(),
              channel);
        });
  }
}
