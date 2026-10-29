package com.bdo.incidencias.domain.model;

import com.bdo.incidencias.domain.exception.IncidenciaCerradaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private Estado estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    /**
     * Carga inmediata: la API siempre devuelve el usuario asignado junto a la incidencia y la respuesta
     * se construye fuera de la transacción (open-in-view desactivado).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_asignado_id")
    private Usuario usuarioAsignado;

    protected Incidencia() {
    }

    /**
     * Toda incidencia nace en estado ABIERTA con la fecha de creación del momento.
     */
    public Incidencia(String titulo, String descripcion, Prioridad prioridad, Usuario usuarioAsignado) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.usuarioAsignado = usuarioAsignado;
        this.estado = Estado.ABIERTA;
        this.fechaCreacion = Instant.now();
    }

    /**
     * Regla de negocio: una incidencia CERRADA no puede cambiar de estado (no se reabre).
     */
    public void cambiarEstado(Estado nuevoEstado) {
        if (this.estado == Estado.CERRADA) {
            throw new IncidenciaCerradaException(id);
        }
        this.estado = nuevoEstado;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Usuario getUsuarioAsignado() {
        return usuarioAsignado;
    }
}
