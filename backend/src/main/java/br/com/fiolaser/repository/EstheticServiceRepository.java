package br.com.fiolaser.repository;

import br.com.fiolaser.model.EstheticService;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EstheticServiceRepository extends JpaRepository<EstheticService, String> {
  List<EstheticService> findByAtivoTrue();

  Optional<EstheticService> findBySlug(String slug);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from EstheticService s where s.id=:id")
  Optional<EstheticService> findLockedById(@Param("id") String id);
}
