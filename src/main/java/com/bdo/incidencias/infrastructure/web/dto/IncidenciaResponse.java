package com.bdo.incidencias.infrastructure.web.dto;

import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import com.bdo.incidencias.domain.model.Prioridad;

import java.time.LocalDateTime;

public record IncidenciaResponse(
    Long id,
    String titulo,
    String descripcion,
    Prioridad prioridad,
    Estado estado,
    LocalDateTime fechaCreacion,
    UsuarioResponse usuarioAsignado
) {

    public static IncidenciaResponse from(Incidencia incidencia) {
        UsuarioResponse usuario = incidencia.getUsuarioAsignado() == null
            ? null
            : UsuarioResponse.from(incidencia.getUsuarioAsignado());
        return new IncidenciaResponse(
            incidencia.getId(),
            incidencia.getTitulo(),
            incidencia.getDescripcion(),
            incidencia.getPrioridad(),
            incidencia.getEstado(),
            incidencia.getFechaCreacion(),
            usuario);
    }
}
