package com.parktech.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa la tabla "tarifas" (HU-02).
 * Hay una fila por tipo de vehículo con su valor por hora en pesos colombianos.
 */
@Entity
@Table(name = "tarifas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MOTO o CARRO. Solo puede existir una tarifa por tipo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, unique = true, length = 10)
    private TipoVehiculo tipoVehiculo;

    /** Valor de una hora en pesos (sin centavos). Ej: 2000 = $2.000. */
    @Column(name = "valor_hora", nullable = false)
    private Long valorHora;
}
