package br.com.fiolaser.security;

import br.com.fiolaser.dto.AuthResponse;
import br.com.fiolaser.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthCodeService {
  private final JdbcTemplate jdbc;
  private final AuthService auth;
  private final SecureRandom random = new SecureRandom();

  public OAuthCodeService(JdbcTemplate jdbc, AuthService auth) {
    this.jdbc = jdbc;
    this.auth = auth;
  }

  @Transactional
  public String emitir(String userId) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    jdbc.update("DELETE FROM oauth_login_codes WHERE expires_at < now()");
    jdbc.update(
        "INSERT INTO oauth_login_codes(code_hash,user_id,expires_at) VALUES (?,?,now()+interval '60 seconds')",
        hash(code),
        userId);
    return code;
  }

  @Transactional
  public AuthResponse trocar(String code) {
    if (code == null || !code.matches("[A-Za-z0-9_-]{43}"))
      throw new IllegalArgumentException("Código inválido ou expirado.");
    List<String> ids =
        jdbc.query(
            "DELETE FROM oauth_login_codes WHERE code_hash=? AND expires_at>now() RETURNING user_id",
            (rs, n) -> rs.getString(1),
            hash(code));
    if (ids.isEmpty()) throw new IllegalArgumentException("Código inválido ou expirado.");
    return auth.resposta(auth.usuario(ids.getFirst()));
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
