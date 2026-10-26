package br.com.fiolaser.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expiration;

  public JwtService(
      @Value("${app.security.jwt.secret}") String secret,
      @Value("${app.security.jwt.expiration-ms}") long expiration) {
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }

  public String gerarToken(String id, String role, long version) {
    return Jwts.builder()
        .issuer("fiolaser-api")
        .audience().add("fiolaser-web").and()
        .subject(id)
        .claim("role", role)
        .claim("version", version)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).requireIssuer("fiolaser-api").requireAudience("fiolaser-web").build().parseSignedClaims(token).getPayload();
  }
}
