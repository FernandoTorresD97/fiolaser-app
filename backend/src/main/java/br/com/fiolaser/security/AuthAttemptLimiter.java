package br.com.fiolaser.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Shared database counters. IP alone is not trusted: login also limits the identifier. */
@Service
public class AuthAttemptLimiter {
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final byte[] key;
  public AuthAttemptLimiter(JdbcTemplate jdbc, Clock clock, @Value("${app.security.jwt.secret}") String secret) {
    this.jdbc=jdbc;this.clock=clock;this.key=secret.getBytes(StandardCharsets.UTF_8);
  }
  @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void check(String operation,String address,String identifier) {
    long now=clock.instant().getEpochSecond();
    jdbc.update("delete from auth_attempt_buckets where window_start < ?",now-3600);
    count(operation+"|ip|"+address,now,60,30);
    if(identifier!=null) {
      String normalized=identifier.trim().toLowerCase(java.util.Locale.ROOT);
      if(normalized.matches("[+0-9 ()-]{10,20}")) normalized=normalized.replaceAll("[^0-9]","");
      count(operation+"|account|"+normalized,now,300,10);
    }
  }
  private void count(String identity,long now,long seconds,int limit) {
    long window=now/seconds*seconds;
    Integer attempts=jdbc.queryForObject("""
        insert into auth_attempt_buckets(bucket_key,window_start,attempts) values (?,?,1)
        on conflict(bucket_key) do update set
          attempts=case when auth_attempt_buckets.window_start=excluded.window_start then auth_attempt_buckets.attempts+1 else 1 end,
          window_start=excluded.window_start returning attempts
        """,Integer.class,hash(identity),window);
    if(attempts!=null && attempts>limit) throw new AuthThrottledException(seconds-(now%seconds));
  }
  private String hash(String value) {
    try {var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
    catch(java.security.GeneralSecurityException ex){throw new IllegalStateException("Authentication limiter unavailable");}
  }
}
