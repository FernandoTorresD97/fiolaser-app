package br.com.fiolaser.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dado sensivel: o CPF NUNCA e armazenado em texto puro. - cpfHash: hash SHA-256 (com pepper da
 * aplicacao), usado so para checar duplicidade no cadastro. - cpfEncrypted: valor cifrado com
 * AES-GCM, decifrado somente quando estritamente necessario (ex.: emissao de nota fiscal), nunca
 * devolvido em respostas de API por padrao.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String nomeCompleto;

  @Column(unique = true)
  private String email;

  @Column(unique = true)
  private String telefone;

  @Column(unique = true)
  private String cpfHash;

  @Column private String cpfEncrypted;

  @Column private LocalDate dataNascimento;

  // null quando o provider e GOOGLE
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider;

  // id do usuario no Google, quando provider = GOOGLE
  private String providerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private boolean ativo;

  @Column(nullable = false)
  private long tokenVersion;

  public boolean perfilCompleto() {
    return cpfHash != null
        && cpfEncrypted != null
        && !cpfEncrypted.isBlank()
        && dataNascimento != null;
  }

  @Column(nullable = false, updatable = false)
  private Instant criadoEm;

  @PrePersist
  void prePersist() {
    if (criadoEm == null) criadoEm = Instant.now();
    if (role == null) role = Role.CLIENTE;
  }
}
