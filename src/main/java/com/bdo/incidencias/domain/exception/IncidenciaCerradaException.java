package com.bdo.incidencias.domain.exception;

public class IncidenciaCerradaException extends RuntimeException {

    public IncidenciaCerradaException(Long id) {
        super("La incidencia con id %s está cerrada y no puede cambiar de estado.".formatted(id));
    }
}
