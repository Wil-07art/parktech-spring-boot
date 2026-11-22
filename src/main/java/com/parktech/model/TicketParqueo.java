package com.parktech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla "tickets_parqueo" (HU-01).
 * Cada fila es una visita de un vehículo: se crea al ingresar (estado ADENTRO)
 * y se cierra al pagar (estado SALIO, con hora de salida y valor pagado).
 *
 * Los índices aceleran las búsquedas más frecuentes (RNF-01):
 *  - por placa + estado (buscar el vehículo activo)
 *  - por estado + hora de salida (reporte de caja del día)
 */
@Entity
@Table(name = "tickets_parqueo", indexes = {
        @Index(name = "idx_ticket_placa_estado", columnList = "placa, estado"),
        @Index(name = "idx_ticket_estado_salida", columnList = "estado, hora_salida")
})
@Getter
@Setter
@NoArgsConstructor
public class TicketParqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Placa normalizada con guion, por ejemplo ABC-123 o ABC-12D. */
    @Column(nullable = false, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 10)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "hora_ingreso", nullable = false)
    private LocalDateTime horaIngreso;

    /** Queda vacía (null) mientras el vehículo está adentro. */
    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;

    /** Queda vacío (null) mientras el vehículo está adentro. */
    @Column(name = "valor_pagado")
    private Long valorPagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoTicket estado;

    /** Operario que registró el ingreso (columna operario_id, llave foránea a usuarios). */
    @ManyToOne
    @JoinColumn(name = "operario_id", nullable = false)
    private Usuario operario;
}
