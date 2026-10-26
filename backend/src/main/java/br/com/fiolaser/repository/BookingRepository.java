package br.com.fiolaser.repository;

import br.com.fiolaser.model.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, String> {
  List<Booking> findByUserIdOrderByHorarioDesc(String userId);

  @Query(
      "select b from Booking b where b.service.id=:service and b.horario < :end and b.fimHorario > :start and b.status in :statuses")
  List<Booking> overlaps(
      @Param("service") String service,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<BookingStatus> statuses);

  List<Booking> findByHorarioGreaterThanEqualAndHorarioLessThanOrderByHorarioAsc(
      LocalDateTime inicio, LocalDateTime fim);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Booking b where b.id=:id")
  Optional<Booking> findLockedById(@Param("id") String id);
}
