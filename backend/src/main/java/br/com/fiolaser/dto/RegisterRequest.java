package br.com.fiolaser.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank @Size(max = 255) String nomeCompleto,
    @Email @Size(max = 255) String email,
    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{10,20}$") String telefone,
    @NotBlank String cpf,
    @NotBlank @Size(min = 10, max = 72) String senha,
    @NotNull @Past LocalDate dataNascimento) {}
