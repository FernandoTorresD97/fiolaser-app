package br.com.fiolaser.security;

import br.com.fiolaser.service.AuthService;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleSuccessHandler implements AuthenticationSuccessHandler {
  private final AuthService auth;
  private final OAuthCodeService codes;
  private final String frontend;

  public GoogleSuccessHandler(
      AuthService auth,
      OAuthCodeService codes,
      @Value("${app.cors.frontend-url}") String frontend) {
    this.auth = auth;
    this.codes = codes;
    this.frontend = frontend;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest req, HttpServletResponse res, Authentication authentication)
      throws IOException {
    String target = frontend + "/login.html?erro=google";
    try {
      OidcUser principal = (OidcUser) authentication.getPrincipal();
      if (!Boolean.TRUE.equals(principal.getEmailVerified()))
        throw new IllegalArgumentException("E-mail não verificado.");
      var user =
          auth.loginOuRegistrarComGoogle(
              principal.getSubject(), principal.getEmail(), principal.getFullName());
      target = frontend + "/oauth-callback.html#code=" + codes.emitir(user.getId());
    } catch (IllegalArgumentException
        | org.springframework.dao.DataIntegrityViolationException ignored) {
      target = frontend + "/login.html?erro=google";
    } finally {
      var session = req.getSession(false);
      if (session != null) session.invalidate();
      SecurityContextHolder.clearContext();
    }
    res.setHeader("Cache-Control", "no-store");
    res.setHeader("Referrer-Policy", "no-referrer");
    res.sendRedirect(target);
  }
}
