package com.bdo.incidencias.infrastructure.persistence;

import com.bdo.incidencias.domain.model.Estado;
import com.bdo.incidencias.domain.model.Incidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    // El usuario asignado se carga en la misma consulta (JOIN) para evitar una consulta por fila en los listados.
    @EntityGraph(attributePaths = "usuarioAsignado")
    List<Incidencia> findAllByOrderByFechaCreacionDescIdDesc();

    @EntityGraph(attributePaths = "usuarioAsignado")
    List<Incidencia> findAllByEstadoOrderByFechaCreacionDescIdDesc(Estado estado);
}
