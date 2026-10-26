package br.com.fiolaser.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CompleteProfileRequest(
    @NotBlank String cpf,
    @NotNull @Past LocalDate dataNascimento,
    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{10,20}$") String telefone) {}
