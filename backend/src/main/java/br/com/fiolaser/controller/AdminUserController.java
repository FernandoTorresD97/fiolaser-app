package br.com.fiolaser.controller;

import br.com.fiolaser.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
  private final AdminUserService service;

  public AdminUserController(AdminUserService service) {
    this.service = service;
  }

  @GetMapping
  public List<AdminUserService.UserView> list(@RequestParam(defaultValue = "") String q) {
    return service.listar(q);
  }

  @PutMapping("/{id}")
  public AdminUserService.UserView update(
      Authentication a, @PathVariable String id, @Valid @RequestBody AdminUserService.Update req) {
    return service.atualizar(a.getName(), id, req);
  }
}
