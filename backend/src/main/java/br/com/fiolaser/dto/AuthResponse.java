package br.com.fiolaser.dto;

public record AuthResponse(
    String token, String nomeCompleto, String role, boolean perfilCompleto) {}
