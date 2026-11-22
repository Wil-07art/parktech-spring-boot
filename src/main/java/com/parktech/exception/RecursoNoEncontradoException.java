package com.parktech.exception;

/**
 * Excepción de negocio para cuando algo buscado no existe
 * (por ejemplo, una placa que no está dentro del parqueadero).
 * El GlobalExceptionHandler la convierte en HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
