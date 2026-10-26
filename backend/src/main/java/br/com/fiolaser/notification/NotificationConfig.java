package br.com.fiolaser.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class NotificationConfig {
  @Bean
  public DefaultErrorHandler notificationErrors(KafkaTemplate<String, String> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template);
    recoverer.setFailIfSendResultIsError(true);
    var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 3));
    handler.setLogLevel(org.springframework.kafka.KafkaException.Level.DEBUG);
    return handler;
  }
}
