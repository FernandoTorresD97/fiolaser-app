package br.com.fiolaser.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @jakarta.validation.constraints.Size(max=254) String identificador, // e-mail ou telefone
    @NotBlank @jakarta.validation.constraints.Size(max=128) String senha) {}
