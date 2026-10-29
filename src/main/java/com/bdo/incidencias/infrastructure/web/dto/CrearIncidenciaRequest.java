package com.bdo.incidencias.infrastructure.web.dto;

import com.bdo.incidencias.domain.model.Prioridad;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearIncidenciaRequest(
    @NotBlank(message = "es obligatorio")
    @Size(max = 100, message = "no puede superar {max} caracteres")
    @Schema(example = "La VPN no conecta desde casa")
    String titulo,

    @Size(max = 1000, message = "no puede superar {max} caracteres")
    @Schema(example = "Error de autenticación al conectar a la VPN corporativa.")
    String descripcion,

    @NotNull(message = "es obligatoria")
    Prioridad prioridad,

    @Schema(description = "Id del usuario asignado. Opcional.", example = "1")
    Long usuarioAsignadoId
) {
}
