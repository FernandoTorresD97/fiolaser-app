package br.com.fiolaser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionGuard {
  public ProductionGuard(
      @Value("${app.security.jwt.secret}") String jwt,
      @Value("${app.security.cpf.pepper}") String pepper,
      @Value("${app.security.cpf.aes-key}") String aes,
      @Value("${app.cors.frontend-url}") String frontend) {
    if (jwt.startsWith("troque-")
        || pepper.startsWith("troque-")
        || aes.equals("5C62CK165ZbelsBDNTocFqc3AkqsvyFe9vMxC+za25Y="))
      throw new IllegalStateException("Configure segredos próprios para produção.");
    if (!frontend.startsWith("https://"))
      throw new IllegalStateException("FRONTEND_URL deve usar HTTPS em produção.");
  }
}
