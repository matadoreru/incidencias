package com.bdo.incidencias.infrastructure.web.dto;

import com.bdo.incidencias.domain.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String email) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
