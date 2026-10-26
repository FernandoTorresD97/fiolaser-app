package br.com.fiolaser.repository;

import br.com.fiolaser.model.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByTelefone(String telefone);

  Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

  boolean existsByCpfHash(String cpfHash);

  boolean existsByEmail(String email);

  boolean existsByTelefone(String telefone);

  long countByRoleAndAtivoTrue(Role role);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id=:id")
  Optional<User> findLockedById(@Param("id") String id);

  List<User>
      findTop100ByNomeCompletoContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeCompleto(
          String nome, String email);
}
