package com.bdo.incidencias.application;

import com.bdo.incidencias.domain.exception.IncidenciaNoEncontradaException;
import com.bdo.incidencias.domain.exception.UsuarioNoEncontradoException;
import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import com.bdo.incidencias.domain.model.Prioridad;
import com.bdo.incidencias.domain.model.Usuario;
import com.bdo.incidencias.infrastructure.persistence.IncidenciaRepository;
import com.bdo.incidencias.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final UsuarioRepository usuarioRepository;

    public IncidenciaService(IncidenciaRepository incidenciaRepository, UsuarioRepository usuarioRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Crea una incidencia en estado ABIERTA. El usuario asignado es opcional (id nulo).
     */
    @Transactional
    public Incidencia crear(String titulo, String descripcion, Prioridad prioridad, Long usuarioAsignadoId) {
        Usuario usuario = null;
        if (usuarioAsignadoId != null) {
            usuario = usuarioRepository.findById(usuarioAsignadoId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioAsignadoId));
        }
        return incidenciaRepository.save(new Incidencia(titulo, descripcion, prioridad, usuario));
    }

    /**
     * Lista las incidencias, más recientes primero. Con estado nulo devuelve todas.
     */
    @Transactional(readOnly = true)
    public List<Incidencia> listar(Estado estado) {
        if (estado == null) {
            return incidenciaRepository.findAllByOrderByFechaCreacionDescIdDesc();
        }
        return incidenciaRepository.findAllByEstadoOrderByFechaCreacionDescIdDesc(estado);
    }

    @Transactional(readOnly = true)
    public Incidencia obtener(Long id) {
        return incidenciaRepository.findById(id)
            .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
    }

    /**
     * Cambia el estado. La regla de las incidencias cerradas vive en la entidad; el cambio se
     * persiste al cerrar la transacción (dirty checking de JPA).
     */
    @Transactional
    public Incidencia cambiarEstado(Long id, Estado nuevoEstado) {
        Incidencia incidencia = incidenciaRepository.findById(id)
            .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
        incidencia.cambiarEstado(nuevoEstado);
        return incidencia;
    }
}
