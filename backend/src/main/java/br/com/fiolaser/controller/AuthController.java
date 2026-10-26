package br.com.fiolaser.controller;

import br.com.fiolaser.dto.*;
import br.com.fiolaser.security.OAuthCodeService;
import br.com.fiolaser.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final OAuthCodeService codes;
  private final br.com.fiolaser.security.AuthAttemptLimiter limiter;

  public AuthController(AuthService auth, OAuthCodeService codes, br.com.fiolaser.security.AuthAttemptLimiter limiter) {
    this.auth = auth;
    this.codes = codes;
    this.limiter = limiter;
  }

  @PostMapping("/registrar")
  public AuthResponse registrar(@Valid @RequestBody RegisterRequest req, jakarta.servlet.http.HttpServletRequest request) {
    limiter.check("register",request.getRemoteAddr(),null);
    return auth.registrar(req);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest req, jakarta.servlet.http.HttpServletRequest request) {
    limiter.check("login",request.getRemoteAddr(),req.identificador());
    return auth.login(req);
  }

  public record Exchange(@NotBlank @Size(max = 100) String code) {}

  @PostMapping("/google/exchange")
  public AuthResponse exchange(@Valid @RequestBody Exchange req, jakarta.servlet.http.HttpServletRequest request) {
    limiter.check("exchange",request.getRemoteAddr(),null);
    return codes.trocar(req.code());
  }

  @GetMapping("/me")
  public Object me(Authentication a) {
    var user = auth.usuario(a.getName());
    return java.util.Map.of(
        "id",
        user.getId(),
        "nomeCompleto",
        user.getNomeCompleto(),
        "role",
        user.getRole(),
        "perfilCompleto",
        user.perfilCompleto(),
        "provider",
        user.getProvider());
  }

  @PostMapping("/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void logout(Authentication a){auth.revogarAcessos(a.getName());}

  @PutMapping("/perfil")
  public AuthResponse completar(Authentication a, @Valid @RequestBody CompleteProfileRequest req) {
    return auth.completar(a.getName(), req);
  }

  @PutMapping("/senha")
  public AuthResponse senha(Authentication a, @Valid @RequestBody ChangePasswordRequest req) {
    return auth.trocarSenha(a.getName(), req);
  }
}
