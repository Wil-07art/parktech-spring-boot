package com.parktech.dto;

/**
 * Respuesta del login: solo los datos que el frontend guarda en sessionStorage.
 * Nunca se devuelve la contraseña.
 */
public record LoginResponse(Long id, String nombre, String rol) {
}
