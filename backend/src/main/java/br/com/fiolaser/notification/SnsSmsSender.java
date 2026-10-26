package br.com.fiolaser.notification;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

@Component
@ConditionalOnProperty(name = "app.notifications.sms-enabled", havingValue = "true")
public class SnsSmsSender implements SmsSender, AutoCloseable {
  private final SnsClient sns = SnsClient.create();

  @Override
  public void send(String phone, String message) {
    sns.publish(
        PublishRequest.builder()
            .phoneNumber(normalize(phone))
            .message(message)
            .messageAttributes(
                Map.of(
                    "AWS.SNS.SMS.SMSType",
                    MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("Transactional")
                        .build()))
            .build());
  }

  public static String normalize(String phone) {
    if (phone == null) throw new IllegalArgumentException("Telefone ausente.");
    String digits = phone.replaceAll("\\D", "");
    String normalized =
        phone.startsWith("+")
            ? "+" + digits
            : (digits.length() == 10 || digits.length() == 11 ? "+55" + digits : "+" + digits);
    if (!normalized.matches("\\+[1-9][0-9]{7,14}"))
      throw new IllegalArgumentException("Telefone inválido para SMS.");
    return normalized;
  }

  @Override
  public void close() {
    sns.close();
  }
}
