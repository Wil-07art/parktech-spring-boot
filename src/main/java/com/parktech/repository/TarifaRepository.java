package com.parktech.repository;

import com.parktech.model.Tarifa;
import com.parktech.model.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de la tabla "tarifas".
 */
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    /** SELECT * FROM tarifas WHERE tipo_vehiculo = ? */
    Optional<Tarifa> findByTipoVehiculo(TipoVehiculo tipoVehiculo);
}
