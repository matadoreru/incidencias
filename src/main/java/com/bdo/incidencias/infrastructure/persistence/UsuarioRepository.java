package com.bdo.incidencias.infrastructure.persistence;

import com.bdo.incidencias.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByOrderByNombreAsc();
}
