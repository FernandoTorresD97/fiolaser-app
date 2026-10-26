package br.com.fiolaser.controller;

import br.com.fiolaser.dto.ServiceResponse;
import br.com.fiolaser.model.EstheticService;
import br.com.fiolaser.repository.EstheticServiceRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

  private final EstheticServiceRepository repository;

  public ServiceController(EstheticServiceRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<ServiceResponse> listar() {
    return repository.findByAtivoTrue().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{slug}")
  public ResponseEntity<ServiceResponse> buscarPorSlug(@PathVariable String slug) {
    return repository
        .findBySlug(slug)
        .map(this::toResponse)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ServiceResponse toResponse(EstheticService s) {
    return new ServiceResponse(
        s.getId(), s.getSlug(), s.getNome(), s.getDescricao(), s.getDuracaoMinutos(), s.getPreco());
  }
}
