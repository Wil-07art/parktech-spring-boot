package com.parktech.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Manejador global de errores.
 * Atrapa las excepciones que lanzan los servicios y las convierte en un JSON
 * sencillo { "mensaje": "..." } con el código HTTP correcto.
 *
 *  IllegalArgumentException      -> 400 Bad Request  (datos inválidos)
 *  RecursoNoEncontradoException  -> 404 Not Found    (no existe)
 *  IllegalStateException         -> 409 Conflict     (regla de negocio violada, ej. RN-02)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(IllegalArgumentException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(IllegalStateException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Errores de las anotaciones @NotBlank / @NotNull de los DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Datos inválidos"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** JSON mal formado o tipo de vehículo que no es MOTO ni CARRO. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida: revise el formato de los datos (tipo debe ser MOTO o CARRO)");
    }

    /** Parámetros de URL con formato incorrecto, por ejemplo una fecha mal escrita. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getName());
    }

    /** Método auxiliar que arma la respuesta JSON. */
    private ResponseEntity<Map<String, String>> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }
}
