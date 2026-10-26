package br.com.fiolaser.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * O CPF e um dado sensivel (LGPD). Nunca e persistido em texto puro: - hash(cpf + pepper) -> usado
 * so para checar duplicidade de cadastro, e irreversivel. - encrypt(cpf) -> AES-256-GCM, usado
 * apenas se um dia for preciso recuperar o valor original (ex: emissao de documento fiscal), nunca
 * exposto em respostas de API.
 */
@Component
public class CpfProtector {

  private static final String ALGO = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int IV_LENGTH_BYTES = 12;

  private final String pepper;
  private final SecretKeySpec aesKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public CpfProtector(
      @Value("${app.security.cpf.pepper}") String pepper,
      @Value("${app.security.cpf.aes-key}") String aesKeyBase64) {
    this.pepper = pepper;
    byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
    this.aesKey = new SecretKeySpec(keyBytes, "AES");
  }

  public String normalizar(String cpf) {
    return cpf.replaceAll("\\D", "");
  }

  public boolean cpfValido(String cpf) {
    if (cpf == null) return false;
    String digitos = normalizar(cpf);
    if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) return false;

    int soma = 0;
    for (int i = 0; i < 9; i++) soma += (digitos.charAt(i) - '0') * (10 - i);
    int d1 = 11 - (soma % 11);
    if (d1 >= 10) d1 = 0;
    if (d1 != digitos.charAt(9) - '0') return false;

    soma = 0;
    for (int i = 0; i < 10; i++) soma += (digitos.charAt(i) - '0') * (11 - i);
    int d2 = 11 - (soma % 11);
    if (d2 >= 10) d2 = 0;
    return d2 == digitos.charAt(10) - '0';
  }

  public String hash(String cpf) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest((normalizar(cpf) + pepper).getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashBytes);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao gerar hash do CPF", e);
    }
  }

  public String encrypt(String cpf) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGO);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] cipherText = cipher.doFinal(normalizar(cpf).getBytes(StandardCharsets.UTF_8));

      byte[] result = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
      return Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao cifrar CPF", e);
    }
  }
}
