'use strict';

const API = '/api/incidencias';

const tabla = document.querySelector('#tabla-incidencias tbody');
const filtroEstado = document.querySelector('#filtro-estado');
const mensaje = document.querySelector('#mensaje');

const ESTADOS = { ABIERTA: 'Abierta', EN_CURSO: 'En curso', CERRADA: 'Cerrada' };
const PRIORIDADES = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta' };

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = 'mensaje' + (tipo === 'ok' ? ' ok' : '');
    mensaje.hidden = false;
}

function ocultarMensaje() {
    mensaje.hidden = true;
}

function formatearFecha(iso) {
    return new Date(iso).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' });
}

// Extrae un texto legible de un ProblemDetail de la API; si no hay cuerpo, usa el código HTTP.
async function describirError(respuesta) {
    try {
        const problema = await respuesta.json();
        const campos = problema.errors
            ? ' ' + Object.entries(problema.errors).map(([campo, error]) => `${campo}: ${error}`).join('; ')
            : '';
        return (problema.detail || problema.title || `Error ${respuesta.status}`) + campos;
    } catch (e) {
        return `Error ${respuesta.status} al llamar a la API`;
    }
}

function pintarTabla(incidencias) {
    tabla.replaceChildren();
    if (incidencias.length === 0) {
        const fila = tabla.insertRow();
        const celda = fila.insertCell();
        celda.colSpan = 6;
        celda.className = 'vacio';
        celda.textContent = 'No hay incidencias con ese estado.';
        return;
    }
    for (const incidencia of incidencias) {
        const fila = tabla.insertRow();
        fila.insertCell().textContent = incidencia.id;
        fila.insertCell().textContent = incidencia.titulo;
        fila.insertCell().textContent = PRIORIDADES[incidencia.prioridad] || incidencia.prioridad;
        fila.insertCell().textContent = ESTADOS[incidencia.estado] || incidencia.estado;
        fila.insertCell().textContent = formatearFecha(incidencia.fechaCreacion);
        fila.insertCell().textContent = incidencia.usuarioAsignado ? incidencia.usuarioAsignado.nombre : 'Sin asignar';
    }
}

async function cargarIncidencias() {
    const estado = filtroEstado.value;
    const url = estado ? `${API}?estado=${encodeURIComponent(estado)}` : API;
    try {
        const respuesta = await fetch(url);
        if (!respuesta.ok) {
            throw new Error(await describirError(respuesta));
        }
        pintarTabla(await respuesta.json());
        ocultarMensaje();
    } catch (error) {
        tabla.replaceChildren();
        const detalle = error instanceof TypeError
            ? 'No se ha podido conectar con la API. Comprueba que la aplicación está arrancada.'
            : error.message;
        mostrarMensaje('No se han podido cargar las incidencias. ' + detalle);
    }
}

filtroEstado.addEventListener('change', cargarIncidencias);

cargarIncidencias();
