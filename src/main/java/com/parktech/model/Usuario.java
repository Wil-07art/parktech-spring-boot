package com.parktech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa la tabla "usuarios".
 * Guarda las personas que pueden iniciar sesión (administradores y operarios).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Nombre de usuario para el login; no se puede repetir. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Contraseña del usuario.
     * Nota académica: se guarda en texto plano para simplificar el proyecto.
     * En un sistema real se debe guardar cifrada (por ejemplo con BCrypt).
     * @JsonIgnore evita que la contraseña viaje en las respuestas JSON.
     */
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;
}
