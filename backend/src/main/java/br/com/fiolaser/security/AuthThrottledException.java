package br.com.fiolaser.security;
public class AuthThrottledException extends RuntimeException {
  private final long retryAfter;
  public AuthThrottledException(long retryAfter){super("Muitas tentativas. Aguarde antes de tentar novamente.");this.retryAfter=retryAfter;}
  public long retryAfter(){return retryAfter;}
}
