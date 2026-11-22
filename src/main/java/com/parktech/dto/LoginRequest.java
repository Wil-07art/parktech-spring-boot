package com.parktech.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos que envía el formulario de login: { "username": "...", "password": "..." }.
 * Un "record" es una clase inmutable de Java 16+ que ya trae constructor y getters.
 */
public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio") String username,
        @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
