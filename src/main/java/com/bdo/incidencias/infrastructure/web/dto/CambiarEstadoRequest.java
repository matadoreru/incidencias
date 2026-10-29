package com.bdo.incidencias.infrastructure.web.dto;

import com.bdo.incidencias.domain.model.Estado;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
    @NotNull
    Estado estado
) {
}
