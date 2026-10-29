# Gestión de incidencias

Solución a la prueba técnica de BDO (Desarrollador/a Java · perfil Middle) de Hortensi Martin Florit.

Pequeña aplicación web para registrar y consultar incidencias técnicas de un equipo de soporte: API REST con
Spring Boot, PostgreSQL y un frontend en HTML, CSS y JavaScript sin frameworks.

- **Stack**: Java 17, Spring Boot 4.1, Spring Data JPA, Bean Validation, Flyway, PostgreSQL 17, springdoc-openapi.
- **Tests**: JUnit 5 + Mockito para el servicio y `@WebMvcTest` para el contrato HTTP. No necesitan Docker.
- **Arranque**: un único comando con Docker Compose.

## Requisitos

- Docker con Docker Compose (para la base de datos y, opcionalmente, la aplicación).
- Para compilar o ejecutar los tests en local: cualquier JDK reciente. El wrapper de Gradle descarga
  automáticamente un JDK 17 (toolchain) si no hay uno instalado.

## Cómo arrancar

```bash
docker compose up --build
```

Con eso:

- Frontend: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

Para desarrollar con la aplicación fuera de Docker:

```bash
docker compose up -d db      # solo PostgreSQL
./gradlew bootRun
```

La conexión se configura con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD` (por defecto
`jdbc:postgresql://localhost:5432/incidencias`, usuario y contraseña `incidencias`).

Para limpiar contenedores y volúmenes: `docker compose down -v`.

## Cómo ejecutar los tests

```bash
./gradlew test
```

Son tests unitarios y de capa web con mocks: no necesitan Docker ni base de datos.

| Clase                     | Qué cubre                                                                                                        |
|---------------------------|------------------------------------------------------------------------------------------------------------------|
| `IncidenciaServiceTest`   | Creación en estado ABIERTA, regla de las incidencias cerradas, consulta por id, incidencia y usuario inexistentes. |
| `IncidenciaControllerTest`| Códigos HTTP (200, 201, 400, 404, 409) y formato JSON de los errores, con el servicio mockeado.                   |

## API

| Método y ruta                        | Descripción                                                             | Respuestas              |
|--------------------------------------|-------------------------------------------------------------------------|-------------------------|
| `POST /api/incidencias`              | Crea una incidencia. Nace en estado `ABIERTA`. Cabecera `Location`.     | 201, 400                |
| `GET /api/incidencias?estado=`       | Lista incidencias, más recientes primero. Filtro opcional por estado.   | 200, 400                |
| `GET /api/incidencias/{id}`          | Devuelve una incidencia (destino de la cabecera `Location` del POST).   | 200, 404                |
| `PATCH /api/incidencias/{id}/estado` | Cambia el estado. Una incidencia `CERRADA` no puede cambiar de estado.  | 200, 400, 404, 409      |
| `GET /api/usuarios`                  | Lista los usuarios a los que se puede asignar una incidencia.           | 200                     |

Valores: `prioridad` ∈ `BAJA | MEDIA | ALTA`; `estado` ∈ `ABIERTA | EN_CURSO | CERRADA`.

```bash
curl -X POST localhost:8080/api/incidencias \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"La VPN no conecta","descripcion":"Error de autenticación","prioridad":"ALTA","usuarioAsignadoId":1}'

curl 'localhost:8080/api/incidencias?estado=ABIERTA'

curl -X PATCH localhost:8080/api/incidencias/1/estado \
  -H 'Content-Type: application/json' \
  -d '{"estado":"EN_CURSO"}'
```

Respuesta de una incidencia:

```json
{
  "id": 1,
  "titulo": "La VPN no conecta desde casa",
  "descripcion": "Varios usuarios reportan error de autenticación al conectar a la VPN corporativa.",
  "prioridad": "ALTA",
  "estado": "ABIERTA",
  "fechaCreacion": "2026-10-20T08:15:00Z",
  "usuarioAsignado": { "id": 1, "nombre": "Ana García", "email": "ana.garcia@soporte.local" }
}
```

### Errores

Todos los errores de la API, incluidos los que genera el propio Spring (JSON ilegible, parámetro con tipo
incorrecto, ruta inexistente), se devuelven como `ProblemDetail` (RFC 9457). Los errores de validación añaden un
mapa `errors` con el campo y el motivo:

```json
{
  "type": "about:blank",
  "title": "Petición inválida",
  "status": 400,
  "detail": "Hay campos con valores no válidos.",
  "instance": "/api/incidencias",
  "errors": { "titulo": "es obligatorio", "prioridad": "es obligatoria" }
}
```

| Código | Cuándo                                                                                                      |
|--------|-------------------------------------------------------------------------------------------------------------|
| 400    | Título vacío o de más de 100 caracteres, prioridad o estado ausentes o desconocidos, usuario asignado inexistente, JSON ilegible. |
| 404    | La incidencia no existe, o la ruta no existe.                                                               |
| 409    | La incidencia está `CERRADA` y se intenta cambiar su estado.                                               |
| 500    | Error no controlado (se registra en el log; el cuerpo no expone detalles internos).                         |

## Base de datos

Un único script, `src/main/resources/db/migration/V1__init.sql`, que Flyway aplica al arrancar:

- `usuario` (`id`, `nombre`, `email` único).
- `incidencia` (`id`, `titulo` ≤ 100 y no vacío, `descripcion`, `prioridad`, `estado`, `fecha_creacion`,
  `usuario_asignado_id` → `usuario.id`), con `CHECK` sobre los valores permitidos de prioridad y estado.
- Índices por `(estado, fecha_creacion)` (consulta del listado filtrado) y por `usuario_asignado_id` (la FK).
- Datos de ejemplo: 3 usuarios y 7 incidencias que cubren todos los estados y prioridades, alguna sin asignar.


## Arquitectura

Paquete raíz `com.bdo.incidencias`, organizado por capas:

| Paquete                      | Contenido                                                                                       |
|------------------------------|-------------------------------------------------------------------------------------------------|
| `domain.model`               | Entidades `Incidencia` y `Usuario`, enums `Estado` y `Prioridad`. La regla de las cerradas vive en `Incidencia.cambiarEstado`. |
| `domain.exception`           | Excepciones de negocio (incidencia no encontrada, incidencia cerrada, usuario no encontrado).    |
| `application`                | `IncidenciaService` y `UsuarioService`: casos de uso y transacciones.                            |
| `infrastructure.persistence` | Repositorios Spring Data JPA.                                                                   |
| `infrastructure.web`         | Controladores REST, DTOs (`record` con Bean Validation), `ApiExceptionHandler`, `OpenApiConfig`. |

Las entidades no salen de la API: los controladores reciben y devuelven DTOs.

## Decisiones técnicas

| Decisión                                   | Por qué                                                                                                                                                 |
|--------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| PostgreSQL en Docker en vez de H2          | El enunciado dice que H2 "es suficiente", no obligatorio. Prefiero la misma base de datos que habría en producción; con Docker Compose arranca igual de fácil. Los tests no dependen de ella. |
| Flyway con un único script                 | Cumple el "único script SQL" del enunciado y además queda versionado y se aplica solo al arrancar.                                                       |
| Spring Boot 4.1 (baseline Java 17)         | La rama 3.5 salió de soporte OSS en junio de 2026. El código no usa nada posterior a Java 17.                                                             |
| Toolchain de Gradle fijado a Java 17       | Garantiza que se compila y ejecuta con Java 17 aunque el equipo tenga otro JDK; Gradle lo descarga si falta. La imagen Docker usa `eclipse-temurin:17`.  |
| Regla de negocio en la entidad             | `Incidencia.cambiarEstado` lanza la excepción si está cerrada. El servicio orquesta; la regla no depende de Spring y se prueba con un test unitario.      |
| `ProblemDetail` para todos los errores     | Formato estándar (RFC 9457) que Spring ya usa para sus propios errores, así la API es coherente sin escribir un formato propio.                            |
| `estado` y `prioridad` como `VARCHAR` + `CHECK` | Más simple que tipos enum de PostgreSQL y compatible con `ddl-auto: validate`; en Java son enums (`@Enumerated(STRING)`).                              |
| `fecha_creacion` como `TIMESTAMPTZ` / `Instant` | La aplicación en Docker corre en UTC; con zona horaria explícita el navegador muestra la hora local correcta.                                            |
| Relación con el usuario cargada con la incidencia | La API siempre devuelve el usuario asignado y la respuesta se construye fuera de la transacción (`open-in-view: false`).                                 |
| Sin paginación                             | No la pide el enunciado y la lista es pequeña. Sería lo primero a añadir si creciera.                                                                    |
| Tests sin base de datos                    | El enunciado pide tests unitarios de servicio; con Mockito se prueban las reglas sin levantar nada. El contrato HTTP se cubre con `@WebMvcTest`.         |

## Qué ha quedado pendiente o mejoraría con más tiempo

- Test de integración contra PostgreSQL real (Testcontainers) que recorra el flujo completo: lo descarté para
  que `./gradlew test` no dependa de Docker y por el tamaño de la prueba.
- Paginación y ordenación en el listado.
- Control de concurrencia en el cambio de estado: dos `PATCH` simultáneos sobre la misma incidencia podrían
  colarse entre la comprobación y la escritura (lost update). La solución sería `@Version` en `Incidencia` y
  mapear el fallo de bloqueo optimista a 409.
- Edición del resto de campos y reasignación de usuario.
- Autenticación.
- En el frontend: cambiar el estado desde la tabla y recargar la lista de usuarios si falla al arrancar.

## Uso de IA

He usado Claude Code (Anthropic) como asistente durante el desarrollo, lo he usado para crear los tests y para hacer la parte de frontal más amigable.
También lo he usado como guía en modo Plan para preguntar si tenía alguna duda.

## Checklist del enunciado

| Requisito                                                                 | Estado                                                                 |
|---------------------------------------------------------------------------|------------------------------------------------------------------------|
| Modelo Incidencia (título ≤ 100, prioridad, estado, fecha, usuario opcional) | Hecho                                                              |
| Modelo Usuario (id, nombre, email)                                        | Hecho                                                                  |
| Un único script SQL con tablas, PK, FK, restricciones y datos de ejemplo  | Hecho: `V1__init.sql`                                                  |
| `POST /api/incidencias` → 201 / 400, estado inicial ABIERTA               | Hecho                                                                  |
| `GET /api/incidencias` con filtro opcional por estado                     | Hecho                                                                  |
| `PATCH /api/incidencias/{id}/estado` → 409 si cerrada, 404 si no existe   | Hecho                                                                  |
| Errores en JSON con formato coherente                                     | Hecho: `ProblemDetail` en toda la API                                  |
| Separación en capas                                                       | Hecho: controlador / servicio / repositorio / dominio                  |
| Dos tests unitarios de servicio con JUnit 5, uno para la regla de cerradas | Hecho: `IncidenciaServiceTest` (8 tests), más `IncidenciaControllerTest` (16) |
| Frontend: tabla con `fetch`                                               | Hecho                                                                  |
| Frontend: filtro por estado sin recargar, mensaje si la API falla         | Hecho                                                                  |
| Frontend (opcional): formulario de creación                               | Hecho                                                                  |
| Java 11 o 17                                                              | Java 17                                                                |
| Base de datos relacional                                                  | PostgreSQL 17 en Docker (el enunciado admite H2; ver decisiones)       |
| README: arranque y tests, decisiones, pendiente, uso de IA                | Este documento                                                         |
