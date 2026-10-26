package br.com.fiolaser.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {
  @Bean
  NewTopic bookingsTopic(@Value("${KAFKA_REPLICATION_FACTOR:1}") int replication) {
    return TopicBuilder.name("booking-created")
        .partitions(3)
        .replicas(replication)
        .config("retention.ms", "604800000")
        .build();
  }

  @Bean
  NewTopic bookingsDeadLetterTopic(@Value("${KAFKA_REPLICATION_FACTOR:1}") int replication) {
    return TopicBuilder.name("booking-created.DLT")
        .partitions(3)
        .replicas(replication)
        .config("retention.ms", "1209600000")
        .build();
  }
}
