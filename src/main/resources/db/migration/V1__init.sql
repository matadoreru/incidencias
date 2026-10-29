-- Esquema y datos de ejemplo de la gestión de incidencias.
-- Único script del enunciado: dos tablas, claves primarias y foránea, restricciones y datos.

CREATE TABLE usuario (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email  VARCHAR(255) NOT NULL,
    CONSTRAINT usuario_email_unico UNIQUE (email)
);

CREATE TABLE incidencia (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo              VARCHAR(100)  NOT NULL,
    descripcion         VARCHAR(1000),
    prioridad           VARCHAR(10)   NOT NULL,
    estado              VARCHAR(10)   NOT NULL,
    fecha_creacion      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_asignado_id BIGINT        REFERENCES usuario (id),
    CONSTRAINT incidencia_titulo_no_vacio CHECK (length(trim(titulo)) > 0),
    CONSTRAINT incidencia_prioridad_valida CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT incidencia_estado_valido CHECK (estado IN ('ABIERTA', 'EN_CURSO', 'CERRADA'))
);

-- El listado se filtra por estado y se ordena por fecha de creación.
CREATE INDEX incidencia_estado_fecha_idx ON incidencia (estado, fecha_creacion DESC);
CREATE INDEX incidencia_usuario_asignado_idx ON incidencia (usuario_asignado_id);

INSERT INTO usuario (nombre, email) VALUES
    ('Ana García',    'ana.garcia@soporte.local'),
    ('Luis Martínez', 'luis.martinez@soporte.local'),
    ('Marta López',   'marta.lopez@soporte.local');

INSERT INTO incidencia (titulo, descripcion, prioridad, estado, fecha_creacion, usuario_asignado_id) VALUES
    ('La VPN no conecta desde casa',
     'Varios usuarios reportan error de autenticación al conectar a la VPN corporativa.',
     'ALTA', 'ABIERTA', TIMESTAMP '2026-10-20 09:15:00', 1),
    ('Impresora de la planta 2 sin tóner',
     NULL,
     'BAJA', 'ABIERTA', TIMESTAMP '2026-10-21 11:40:00', NULL),
    ('Correo lento al abrir adjuntos',
     'Los adjuntos de más de 5 MB tardan más de un minuto en abrirse.',
     'MEDIA', 'EN_CURSO', TIMESTAMP '2026-10-22 08:05:00', 2),
    ('Error 500 al exportar informes',
     'La exportación a PDF del módulo de informes devuelve error desde la última actualización.',
     'ALTA', 'EN_CURSO', TIMESTAMP '2026-10-23 16:30:00', 3),
    ('Solicitud de acceso a la carpeta compartida de RRHH',
     'Alta de permisos para la nueva incorporación.',
     'MEDIA', 'CERRADA', TIMESTAMP '2026-10-24 10:00:00', 1),
    ('Teclado inalámbrico no responde',
     'Sustituidas las pilas; el problema persistía y se ha cambiado el teclado.',
     'BAJA', 'CERRADA', TIMESTAMP '2026-10-26 13:20:00', 2),
    ('Portátil no arranca tras actualización',
     'Pantalla azul en el arranque. Pendiente de diagnóstico.',
     'ALTA', 'ABIERTA', TIMESTAMP '2026-10-28 09:45:00', NULL);
