package com.bdo.incidencias.infrastructure.web;

import com.bdo.incidencias.domain.exception.IncidenciaCerradaException;
import com.bdo.incidencias.domain.exception.IncidenciaNoEncontradaException;
import com.bdo.incidencias.domain.exception.UsuarioNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Todos los errores de la API se devuelven como ProblemDetail (RFC 9457):
 * {type, title, status, detail, instance} y, en errores de validación, un mapa "errors" campo → mensaje.
 * Los errores que ya gestiona Spring los convierte ResponseEntityExceptionHandler al mismo formato; los más
 * habituales (cuerpo ilegible, parámetro con tipo incorrecto, ruta inexistente) se sobreescriben aquí para que
 * título y texto estén en español como el resto.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IncidenciaNoEncontradaException.class)
    public ProblemDetail handleIncidenciaNoEncontrada(IncidenciaNoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, "Incidencia no encontrada", ex.getMessage());
    }

    @ExceptionHandler(IncidenciaCerradaException.class)
    public ProblemDetail handleIncidenciaCerrada(IncidenciaCerradaException ex) {
        return problem(HttpStatus.CONFLICT, "Incidencia cerrada", ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ProblemDetail handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Petición inválida", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Se ha producido un error inesperado.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Petición inválida", "Hay campos con valores no válidos.");

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "valor no válido")));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "El valor '%s' no es válido para '%s'.".formatted(ex.getValue(), ex.getPropertyName());
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Petición inválida", detail));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Petición inválida",
            "El cuerpo de la petición no es un JSON válido o contiene valores no permitidos."));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
        NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND, "Recurso no encontrado",
            "No existe la ruta /%s.".formatted(ex.getResourcePath())));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
