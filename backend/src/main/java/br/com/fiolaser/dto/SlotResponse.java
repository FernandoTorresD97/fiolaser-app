package br.com.fiolaser.dto;

import java.time.LocalDateTime;

// disponivel = true -> horario livre (renderizado com cor normal no frontend)
// disponivel = false -> horario reservado (renderizado opaco/desabilitado)
public record SlotResponse(LocalDateTime horario, boolean disponivel) {}
