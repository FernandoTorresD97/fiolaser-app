package br.com.fiolaser.service;

import br.com.fiolaser.model.*;
import br.com.fiolaser.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
  public record UserView(
      String id,
      String nomeCompleto,
      String email,
      String telefone,
      Role role,
      boolean ativo,
      boolean perfilCompleto) {}

  public record Update(@NotNull Role role, @NotNull Boolean ativo) {}

  private final UserRepository users;
  private final JdbcTemplate jdbc;

  public AdminUserService(UserRepository users, JdbcTemplate jdbc) {
    this.users = users;
    this.jdbc = jdbc;
  }

  public List<UserView> listar(String query) {
    String q = query == null ? "" : query.trim();
    return users
        .findTop100ByNomeCompletoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeCompleto(
            q, q)
        .stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  public UserView atualizar(String actor, String id, Update req) {
    // Serialize administrator changes across all ECS tasks.
    jdbc.queryForObject("SELECT pg_advisory_xact_lock(724361)", Object.class);
    var u =
        users
            .findLockedById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    if (actor.equals(id) && (req.role() != Role.ADMIN || !req.ativo()))
      throw new IllegalArgumentException("Você não pode remover seu próprio acesso.");
    if (u.getRole() == Role.ADMIN
        && u.isAtivo()
        && (req.role() != Role.ADMIN || !req.ativo())
        && users.countByRoleAndAtivoTrue(Role.ADMIN) <= 1)
      throw new IllegalArgumentException("Mantenha pelo menos um administrador ativo.");
    if (u.getRole() != req.role() || u.isAtivo() != req.ativo())
      u.setTokenVersion(u.getTokenVersion() + 1);
    u.setRole(req.role());
    u.setAtivo(req.ativo());
    return view(u);
  }

  private UserView view(User u) {
    return new UserView(
        u.getId(),
        u.getNomeCompleto(),
        u.getEmail(),
        u.getTelefone(),
        u.getRole(),
        u.isAtivo(),
        u.perfilCompleto());
  }
}
