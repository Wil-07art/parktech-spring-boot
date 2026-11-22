package com.parktech.controller;

import com.parktech.dto.LoginRequest;
import com.parktech.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint de inicio de sesión.
 * POST /api/auth/login  ->  200 { id, nombre, rol }  |  401 { mensaje }
 * El frontend guarda la respuesta en sessionStorage.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest datos) {
        return authService.login(datos.username(), datos.password())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("mensaje", "Usuario o contraseña incorrectos")));
    }
}
