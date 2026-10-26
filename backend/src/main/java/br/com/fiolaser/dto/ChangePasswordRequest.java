package br.com.fiolaser.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
    @NotBlank String senhaAtual, @NotBlank @Size(min = 10, max = 72) String novaSenha) {}
