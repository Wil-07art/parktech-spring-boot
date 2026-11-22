package com.parktech.controller;

import com.parktech.model.Usuario;
import com.parktech.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/usuarios -> lista de usuarios (sin contraseña, gracias a @JsonIgnore).
 * El Dashboard de Caja la usa para llenar el filtro de operario.
 * Al ser solo una lectura sin reglas de negocio, consulta el repositorio directamente.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }
}
