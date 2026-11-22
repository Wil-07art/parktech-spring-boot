package com.parktech.model;

/**
 * Estados posibles de un ticket de parqueo.
 * ADENTRO: el vehículo está en el parqueadero (ticket abierto).
 * SALIO:   el vehículo ya pagó y salió (ticket cerrado).
 */
public enum EstadoTicket {
    ADENTRO,
    SALIO
}
