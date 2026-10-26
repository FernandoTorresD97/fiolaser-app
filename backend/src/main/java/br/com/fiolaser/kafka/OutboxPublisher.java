package br.com.fiolaser.kafka;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate tx;
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(OutboxPublisher.class);

  public OutboxPublisher(
      JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.kafka = kafka;
    this.tx = new TransactionTemplate(manager);
  }

  @Scheduled(fixedDelayString = "${app.outbox.delay-ms:3000}")
  public void publish() {
    try {
      tx.executeWithoutResult(
          status -> {
            var rows =
                jdbc.queryForList(
                    "SELECT id,payload FROM booking_outbox WHERE publicado_em IS NULL ORDER BY criado_em LIMIT 20 FOR UPDATE SKIP LOCKED");
            for (var row : rows) {
              try {
                kafka
                    .send(
                        "booking-created", row.get("id").toString(), row.get("payload").toString())
                    .get(10, TimeUnit.SECONDS);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Envio interrompido", e);
              } catch (Exception e) {
                throw new IllegalStateException("Kafka indisponível", e);
              }
              jdbc.update("UPDATE booking_outbox SET publicado_em=now() WHERE id=?", row.get("id"));
            }
          });
    } catch (Exception e) {
      log.warn(
          "Outbox pendente; nova tentativa no próximo ciclo ({})", e.getClass().getSimpleName());
    }
  }
}
