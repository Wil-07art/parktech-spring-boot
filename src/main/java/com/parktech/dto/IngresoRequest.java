package com.parktech.dto;

import com.parktech.model.TipoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para registrar un ingreso (RF-01):
 * { "placa": "ABC123", "tipoVehiculo": "CARRO", "operarioId": 2 }
 */
public record IngresoRequest(
        @NotBlank(message = "La placa es obligatoria") String placa,
        @NotNull(message = "El tipo de vehículo es obligatorio") TipoVehiculo tipoVehiculo,
        @NotNull(message = "El operario es obligatorio") Long operarioId
) {
}
