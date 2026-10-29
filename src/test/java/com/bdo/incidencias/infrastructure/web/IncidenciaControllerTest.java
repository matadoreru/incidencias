package com.bdo.incidencias.infrastructure.web;

import com.bdo.incidencias.application.IncidenciaService;
import com.bdo.incidencias.domain.exception.IncidenciaCerradaException;
import com.bdo.incidencias.domain.exception.IncidenciaNoEncontradaException;
import com.bdo.incidencias.domain.exception.UsuarioNoEncontradoException;
import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import com.bdo.incidencias.domain.model.Prioridad;
import com.bdo.incidencias.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato HTTP del controlador: códigos de estado y formato JSON de los errores.
 * La lógica de negocio está mockeada; se prueba en IncidenciaServiceTest.
 */
@WebMvcTest(IncidenciaController.class)
class IncidenciaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IncidenciaService incidenciaService;

    private static Incidencia incidencia(long id, Estado estado, Usuario usuario) {
        Incidencia incidencia = new Incidencia("VPN caída", "No conecta", Prioridad.ALTA, usuario);
        ReflectionTestUtils.setField(incidencia, "id", id);
        if (estado != Estado.ABIERTA) {
            incidencia.cambiarEstado(estado);
        }
        return incidencia;
    }

    // ---- POST /api/incidencias ----

    @Test
    void crearDevuelve201ConLocationYLaIncidenciaAbierta() {
        Usuario ana = new Usuario("Ana García", "ana@soporte.local");
        ReflectionTestUtils.setField(ana, "id", 1L);
        when(incidenciaService.crear("VPN caída", "No conecta", Prioridad.ALTA, 1L))
            .thenReturn(incidencia(10L, Estado.ABIERTA, ana));

        MvcTestResult result = post("""
            {"titulo": "VPN caída", "descripcion": "No conecta", "prioridad": "ALTA", "usuarioAsignadoId": 1}
            """);

        assertThat(result).hasStatus(HttpStatus.CREATED)
            .hasHeader("Location", "http://localhost/api/incidencias/10")
            .bodyJson()
            .hasPathSatisfying("$.id", id -> id.assertThat().isEqualTo(10))
            .hasPathSatisfying("$.estado", estado -> estado.assertThat().isEqualTo("ABIERTA"))
            .hasPathSatisfying("$.usuarioAsignado.nombre", nombre -> nombre.assertThat().isEqualTo("Ana García"));
    }

    @Test
    void crearSinTituloDevuelve400ConElCampoInvalido() {
        MvcTestResult result = post("""
            {"titulo": "  ", "prioridad": "ALTA"}
            """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(400))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Petición inválida"))
            .hasPathSatisfying("$.errors.titulo", error -> error.assertThat().isNotNull());
    }

    @Test
    void crearConTituloDeMasDe100CaracteresDevuelve400() {
        MvcTestResult result = post("""
            {"titulo": "%s", "prioridad": "ALTA"}
            """.formatted("a".repeat(101)));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().hasPathSatisfying("$.errors.titulo", error -> error.assertThat().isNotNull());
    }

    @Test
    void crearSinPrioridadDevuelve400() {
        MvcTestResult result = post("""
            {"titulo": "VPN caída"}
            """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().hasPathSatisfying("$.errors.prioridad", error -> error.assertThat().isNotNull());
    }

    @Test
    void crearConPrioridadDesconocidaDevuelve400EnJson() {
        MvcTestResult result = post("""
            {"titulo": "VPN caída", "prioridad": "URGENTE"}
            """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(400))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Petición inválida"));
    }

    @Test
    void crearConUsuarioInexistenteDevuelve400() {
        when(incidenciaService.crear(any(), any(), any(), eq(99L)))
            .thenThrow(new UsuarioNoEncontradoException(99L));

        MvcTestResult result = post("""
            {"titulo": "VPN caída", "prioridad": "ALTA", "usuarioAsignadoId": 99}
            """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .hasPathSatisfying("$.detail", detail -> detail.assertThat().isEqualTo("No existe el usuario con id 99."));
    }

    // ---- GET /api/incidencias ----

    @Test
    void listarSinFiltroDevuelveTodasLasIncidencias() {
        when(incidenciaService.listar(isNull()))
            .thenReturn(List.of(incidencia(1L, Estado.ABIERTA, null), incidencia(2L, Estado.CERRADA, null)));

        MvcTestResult result = mvc.get().uri("/api/incidencias").exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.length()", size -> size.assertThat().isEqualTo(2))
            .hasPathSatisfying("$[1].usuarioAsignado", usuario -> usuario.assertThat().isNull());
    }

    @Test
    void listarConFiltroPasaElEstadoAlServicio() {
        when(incidenciaService.listar(Estado.CERRADA)).thenReturn(List.of(incidencia(2L, Estado.CERRADA, null)));

        MvcTestResult result = mvc.get().uri("/api/incidencias").param("estado", "CERRADA").exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
            .bodyJson().hasPathSatisfying("$[0].estado", estado -> estado.assertThat().isEqualTo("CERRADA"));
        verify(incidenciaService).listar(Estado.CERRADA);
    }

    @Test
    void listarConEstadoDesconocidoDevuelve400EnJson() {
        MvcTestResult result = mvc.get().uri("/api/incidencias").param("estado", "FOO").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(400))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Petición inválida"))
            .hasPathSatisfying("$.detail", detail -> detail.assertThat().asString().contains("estado").contains("FOO"));
    }

    @Test
    void rutaInexistenteDevuelve404EnJson() {
        MvcTestResult result = mvc.get().uri("/api/nada").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(404))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Recurso no encontrado"));
    }

    // ---- GET /api/incidencias/{id} ----

    @Test
    void obtenerDevuelve200ConLaIncidencia() {
        when(incidenciaService.obtener(3L)).thenReturn(incidencia(3L, Estado.EN_CURSO, null));

        MvcTestResult result = mvc.get().uri("/api/incidencias/{id}", 3L).exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.id", id -> id.assertThat().isEqualTo(3))
            .hasPathSatisfying("$.estado", estado -> estado.assertThat().isEqualTo("EN_CURSO"));
    }

    @Test
    void obtenerUnaIncidenciaInexistenteDevuelve404() {
        when(incidenciaService.obtener(404L)).thenThrow(new IncidenciaNoEncontradaException(404L));

        MvcTestResult result = mvc.get().uri("/api/incidencias/{id}", 404L).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson().hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Incidencia no encontrada"));
    }

    // ---- PATCH /api/incidencias/{id}/estado ----

    @Test
    void cambiarEstadoDevuelve200ConLaIncidenciaActualizada() {
        when(incidenciaService.cambiarEstado(1L, Estado.EN_CURSO)).thenReturn(incidencia(1L, Estado.EN_CURSO, null));

        MvcTestResult result = patch(1L, "EN_CURSO");

        assertThat(result).hasStatus(HttpStatus.OK)
            .bodyJson().hasPathSatisfying("$.estado", estado -> estado.assertThat().isEqualTo("EN_CURSO"));
    }

    @Test
    void cambiarEstadoDeUnaIncidenciaCerradaDevuelve409() {
        when(incidenciaService.cambiarEstado(5L, Estado.ABIERTA)).thenThrow(new IncidenciaCerradaException(5L));

        MvcTestResult result = patch(5L, "ABIERTA");

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(409))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Incidencia cerrada"))
            .hasPathSatisfying("$.instance", instance -> instance.assertThat().isEqualTo("/api/incidencias/5/estado"));
    }

    @Test
    void cambiarEstadoDeUnaIncidenciaInexistenteDevuelve404() {
        when(incidenciaService.cambiarEstado(404L, Estado.EN_CURSO)).thenThrow(new IncidenciaNoEncontradaException(404L));

        MvcTestResult result = patch(404L, "EN_CURSO");

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson()
            .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo(404))
            .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Incidencia no encontrada"));
    }

    @Test
    void cambiarEstadoSinEstadoDevuelve400() {
        MvcTestResult result = mvc.patch().uri("/api/incidencias/1/estado")
            .contentType(MediaType.APPLICATION_JSON).content("{}").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().hasPathSatisfying("$.errors.estado", error -> error.assertThat().isNotNull());
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/incidencias").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult patch(long id, String estado) {
        return mvc.patch().uri("/api/incidencias/{id}/estado", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"estado\": \"" + estado + "\"}")
            .exchange();
    }
}
