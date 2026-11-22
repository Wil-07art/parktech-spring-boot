package com.parktech.service;

import com.parktech.dto.LoginResponse;
import com.parktech.model.Usuario;
import com.parktech.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Lógica del inicio de sesión.
 * Valida usuario y contraseña contra la tabla "usuarios".
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    /** Spring inyecta el repositorio por el constructor. */
    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Intenta iniciar sesión.
     * @return los datos de sesión si las credenciales son correctas; vacío si no.
     */
    public Optional<LoginResponse> login(String username, String password) {
        return usuarioRepository.findByUsername(username.trim())
                .filter(usuario -> usuario.getPassword().equals(password))
                .map(this::aRespuesta);
    }

    private LoginResponse aRespuesta(Usuario usuario) {
        return new LoginResponse(usuario.getId(), usuario.getNombre(), usuario.getRol().name());
    }
}
