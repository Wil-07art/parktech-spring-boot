package com.parktech.dto;

import com.parktech.model.EstadoTicket;
import com.parktech.model.TipoVehiculo;

import java.time.LocalDateTime;

/**
 * Tarjeta de liquidación (RF-02): lo que el operario ve antes de cobrar
 * y lo que queda registrado después de confirmar el pago.
 */
public record LiquidacionResponse(
        Long ticketId,
        String placa,
        TipoVehiculo tipoVehiculo,
        LocalDateTime horaIngreso,
        LocalDateTime horaSalida,
        long minutosTranscurridos,
        long horasCobradas,
        long valorHora,
        long totalAPagar,
        EstadoTicket estado
) {
}
