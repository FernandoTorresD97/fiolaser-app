package br.com.fiolaser;

import static org.assertj.core.api.Assertions.*;

import br.com.fiolaser.controller.AdminBookingController;
import br.com.fiolaser.security.CpfProtector;
import org.junit.jupiter.api.Test;

class SecurityUnitTest {
  @Test void jwtRejectsAnotherIssuerEvenWithSameSigningKey() {
    String secret="unit-test-secret-with-at-least-32-bytes-value";
    var jwt=new br.com.fiolaser.security.JwtService(secret,60000);
    String alien=io.jsonwebtoken.Jwts.builder().subject("someone").issuer("different-app").audience().add("fiolaser-web").and()
        .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
    assertThatThrownBy(()->jwt.parse(alien)).isInstanceOf(io.jsonwebtoken.JwtException.class);
    assertThat(jwt.parse(jwt.gerarToken("someone","CLIENTE",0)).getIssuer()).isEqualTo("fiolaser-api");
  }
  @Test
  void csvNeutralizesFormulasAndQuotes() {
    assertThat(AdminBookingController.cell("=SUM(A1)")).isEqualTo("\"'=SUM(A1)\"");
    assertThat(AdminBookingController.cell("  +1")).startsWith("\"'");
    assertThat(AdminBookingController.cell("a\"b")).isEqualTo("\"a\"\"b\"");
  }

  @Test
  void cpfNormalizesAndEncryptsWithRandomIv() {
    var cpf = new CpfProtector("test", "5C62CK165ZbelsBDNTocFqc3AkqsvyFe9vMxC+za25Y=");
    assertThat(cpf.cpfValido("529.982.247-25")).isTrue();
    assertThat(cpf.cpfValido("11111111111")).isFalse();
    assertThat(cpf.hash("529.982.247-25")).isEqualTo(cpf.hash("52998224725"));
    assertThat(cpf.encrypt("52998224725")).isNotEqualTo(cpf.encrypt("52998224725"));
  }

  @Test
  void smsNormalizesBrazilianPhone() {
    assertThat(br.com.fiolaser.notification.SnsSmsSender.normalize("(75) 99999-0000"))
        .isEqualTo("+5575999990000");
    assertThatThrownBy(() -> br.com.fiolaser.notification.SnsSmsSender.normalize("abc"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void csvMultilineFormulaRemainsNeutralized() {
    assertThat(AdminBookingController.cell("=1\nmore")).startsWith("\"'");
  }
}
