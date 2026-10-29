package com.bdo.incidencias.infrastructure.web;

import com.bdo.incidencias.application.IncidenciaService;
import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import com.bdo.incidencias.infrastructure.web.dto.CambiarEstadoRequest;
import com.bdo.incidencias.infrastructure.web.dto.CrearIncidenciaRequest;
import com.bdo.incidencias.infrastructure.web.dto.IncidenciaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
@Tag(name = "Incidencias", description = "Registro y consulta de incidencias técnicas")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    public IncidenciaController(IncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    @Operation(summary = "Crear una incidencia", description = "La incidencia se crea en estado ABIERTA.")
    @ApiResponse(responseCode = "201", description = "Incidencia creada. La cabecera Location apunta a la nueva incidencia.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @PostMapping
    public ResponseEntity<IncidenciaResponse> crear(@Valid @RequestBody CrearIncidenciaRequest request) {
        Incidencia incidencia = incidenciaService.crear(
            request.titulo(), request.descripcion(), request.prioridad(), request.usuarioAsignadoId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(incidencia.getId())
            .toUri();

        return ResponseEntity.created(location).body(IncidenciaResponse.from(incidencia));
    }

    @Operation(summary = "Listar incidencias", description = "Más recientes primero. Filtro opcional por estado.")
    @ApiResponse(responseCode = "200", description = "Listado de incidencias")
    @ApiResponse(responseCode = "400", description = "Estado desconocido")
    @GetMapping
    public List<IncidenciaResponse> listar(@RequestParam(required = false) Estado estado) {
        return incidenciaService.listar(estado).stream()
            .map(IncidenciaResponse::from)
            .toList();
    }

    @Operation(summary = "Consultar una incidencia")
    @ApiResponse(responseCode = "200", description = "Incidencia encontrada")
    @ApiResponse(responseCode = "404", description = "La incidencia no existe")
    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id) {
        return IncidenciaResponse.from(incidenciaService.obtener(id));
    }

    @Operation(summary = "Cambiar el estado de una incidencia",
        description = "Una incidencia CERRADA no puede cambiar de estado.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    @ApiResponse(responseCode = "404", description = "La incidencia no existe")
    @ApiResponse(responseCode = "409", description = "La incidencia está cerrada")
    @PatchMapping("/{id}/estado")
    public IncidenciaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return IncidenciaResponse.from(incidenciaService.cambiarEstado(id, request.estado()));
    }
}
