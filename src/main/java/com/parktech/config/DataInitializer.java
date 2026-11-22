package com.parktech.config;

import com.parktech.model.Rol;
import com.parktech.model.Tarifa;
import com.parktech.model.TipoVehiculo;
import com.parktech.model.Usuario;
import com.parktech.repository.TarifaRepository;
import com.parktech.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga los datos iniciales cada vez que arranca la aplicación.
 * CommandLineRunner ejecuta el método run() justo después de iniciar Spring.
 * Solo inserta lo que todavía no existe, así no se duplican datos al reiniciar.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TarifaRepository tarifaRepository;

    public DataInitializer(UsuarioRepository usuarioRepository, TarifaRepository tarifaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tarifaRepository = tarifaRepository;
    }

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("Administrador", "admin", "admin123", Rol.ADMIN);
        crearUsuarioSiNoExiste("Operario 1", "operario1", "oper123", Rol.OPERARIO);

        crearTarifaSiNoExiste(TipoVehiculo.MOTO, 2000L);
        crearTarifaSiNoExiste(TipoVehiculo.CARRO, 4000L);
    }

    private void crearUsuarioSiNoExiste(String nombre, String username, String password, Rol rol) {
        if (usuarioRepository.findByUsername(username).isEmpty()) {
            usuarioRepository.save(new Usuario(null, nombre, username, password, rol));
        }
    }

    private void crearTarifaSiNoExiste(TipoVehiculo tipo, Long valorHora) {
        if (tarifaRepository.findByTipoVehiculo(tipo).isEmpty()) {
            tarifaRepository.save(new Tarifa(null, tipo, valorHora));
        }
    }
}
