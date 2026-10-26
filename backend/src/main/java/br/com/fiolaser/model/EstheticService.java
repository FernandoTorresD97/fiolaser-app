package br.com.fiolaser.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "esthetic_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstheticService {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String slug; // ex: depilacao-a-laser

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, length = 2000)
  private String descricao;

  @Column(nullable = false)
  private Integer duracaoMinutos;

  @Column(nullable = false)
  private BigDecimal preco;

  @Column(nullable = false)
  private boolean ativo;
}
