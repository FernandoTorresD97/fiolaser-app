package br.com.fiolaser.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um horario reservado (RESERVADO ou CONFIRMADO) torna o slot indisponivel para outros clientes --
 * e o que o calendario do frontend pinta como "opaco".
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "service_id", nullable = false)
  private EstheticService service;

  @Column(nullable = false)
  private LocalDateTime horario;

  @Column(nullable = false)
  private LocalDateTime fimHorario;

  @Column(nullable = false)
  private java.math.BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod formaPagamento;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status;

  @Column(nullable = false, updatable = false)
  private Instant criadoEm;

  @PrePersist
  void prePersist() {
    if (criadoEm == null) criadoEm = Instant.now();
    if (status == null) status = BookingStatus.RESERVADO;
  }
}
