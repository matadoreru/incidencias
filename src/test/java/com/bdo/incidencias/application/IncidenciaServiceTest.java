package com.bdo.incidencias.application;

import com.bdo.incidencias.domain.exception.IncidenciaCerradaException;
import com.bdo.incidencias.domain.exception.IncidenciaNoEncontradaException;
import com.bdo.incidencias.domain.exception.UsuarioNoEncontradoException;
import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import com.bdo.incidencias.domain.model.Prioridad;
import com.bdo.incidencias.domain.model.Usuario;
import com.bdo.incidencias.infrastructure.persistence.IncidenciaRepository;
import com.bdo.incidencias.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private IncidenciaService service;

    @Test
    void crearDevuelveUnaIncidenciaEnEstadoAbierta() {
        Usuario ana = new Usuario("Ana", "ana@soporte.local");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Incidencia creada = service.crear("VPN caída", "No conecta", Prioridad.ALTA, 1L);

        assertThat(creada.getEstado()).isEqualTo(Estado.ABIERTA);
        assertThat(creada.getTitulo()).isEqualTo("VPN caída");
        assertThat(creada.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(creada.getUsuarioAsignado()).isSameAs(ana);
        assertThat(creada.getFechaCreacion()).isNotNull();
        verify(incidenciaRepository).save(creada);
    }

    @Test
    void crearSinUsuarioAsignadoNoConsultaUsuarios() {
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Incidencia creada = service.crear("Impresora sin tóner", null, Prioridad.BAJA, null);

        assertThat(creada.getUsuarioAsignado()).isNull();
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void crearConUsuarioInexistenteLanzaExcepcionYNoGuarda() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear("Título", null, Prioridad.MEDIA, 99L))
            .isInstanceOf(UsuarioNoEncontradoException.class);

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoDeUnaIncidenciaAbiertaLaPasaAEnCurso() {
        Incidencia abierta = new Incidencia("Correo lento", null, Prioridad.MEDIA, null);
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(abierta));

        Incidencia actualizada = service.cambiarEstado(5L, Estado.EN_CURSO);

        assertThat(actualizada.getEstado()).isEqualTo(Estado.EN_CURSO);
    }

    @Test
    void cambiarEstadoDeUnaIncidenciaCerradaLanzaExcepcionYNoCambiaElEstado() {
        Incidencia cerrada = new Incidencia("Acceso a carpeta", null, Prioridad.MEDIA, null);
        cerrada.cambiarEstado(Estado.CERRADA);
        when(incidenciaRepository.findById(7L)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> service.cambiarEstado(7L, Estado.ABIERTA))
            .isInstanceOf(IncidenciaCerradaException.class);

        assertThat(cerrada.getEstado()).isEqualTo(Estado.CERRADA);
    }

    @Test
    void cambiarEstadoDeUnaIncidenciaInexistenteLanzaExcepcion() {
        when(incidenciaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(404L, Estado.EN_CURSO))
            .isInstanceOf(IncidenciaNoEncontradaException.class);
    }
}
