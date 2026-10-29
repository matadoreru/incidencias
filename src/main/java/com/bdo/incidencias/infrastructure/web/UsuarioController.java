package com.bdo.incidencias.infrastructure.web;

import com.bdo.incidencias.application.UsuarioService;
import com.bdo.incidencias.infrastructure.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Usuarios a los que se puede asignar una incidencia")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios", description = "Ordenados por nombre.")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream()
            .map(UsuarioResponse::from)
            .toList();
    }
}
