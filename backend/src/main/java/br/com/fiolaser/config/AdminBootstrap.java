package br.com.fiolaser.config;

import br.com.fiolaser.model.*;
import br.com.fiolaser.repository.UserRepository;
import br.com.fiolaser.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JdbcTemplate jdbc;
  private final String email;
  private final String password;

  public AdminBootstrap(
      UserRepository users,
      PasswordEncoder encoder,
      JdbcTemplate jdbc,
      @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
      @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.jdbc = jdbc;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (email.isBlank() || password.isBlank()) return;
    jdbc.queryForObject("SELECT pg_advisory_xact_lock(724361)", Object.class);
    if (users.countByRoleAndAtivoTrue(Role.ADMIN) > 0) return;
    AuthService.validarSenha(password);
    if (users.findByEmailIgnoreCase(email).isPresent())
      throw new IllegalStateException("Use um e-mail novo para o administrador inicial.");
    users.save(
        User.builder()
            .nomeCompleto("Administração")
            .email(AuthService.email(email))
            .senhaHash(encoder.encode(password))
            .role(Role.ADMIN)
            .provider(AuthProvider.LOCAL)
            .ativo(true)
            .build());
  }
}
