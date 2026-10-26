package br.com.fiolaser.dto;

import java.math.BigDecimal;

public record ServiceResponse(
    String id,
    String slug,
    String nome,
    String descricao,
    Integer duracaoMinutos,
    BigDecimal preco) {}
