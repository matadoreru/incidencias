package com.bdo.incidencias.domain.exception;

public class IncidenciaNoEncontradaException extends RuntimeException {

    public IncidenciaNoEncontradaException(Long id) {
        super("No existe la incidencia con id %s.".formatted(id));
    }
}
