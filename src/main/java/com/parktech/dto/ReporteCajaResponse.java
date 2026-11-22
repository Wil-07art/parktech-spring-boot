package com.parktech.dto;

import com.parktech.model.TicketParqueo;

import java.time.LocalDate;
import java.util.List;

/**
 * Consolidado diario de caja (RF-03 / HU-03).
 * Incluye los totales para las tarjetas KPI y la lista de tickets cerrados.
 */
public record ReporteCajaResponse(
        LocalDate fecha,
        Long operarioId,
        long totalRecaudado,
        long subtotalMotos,
        long subtotalCarros,
        int cantidadTickets,
        List<TicketParqueo> tickets
) {
}
