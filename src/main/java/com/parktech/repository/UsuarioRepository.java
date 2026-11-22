package com.parktech.repository;

import com.parktech.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de la tabla "usuarios".
 * Spring Data JPA genera el SQL automáticamente a partir del nombre del método.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** SELECT * FROM usuarios WHERE username = ? */
    Optional<Usuario> findByUsername(String username);
}
