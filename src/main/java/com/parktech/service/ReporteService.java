package com.parktech.service;

import com.parktech.dto.ReporteCajaResponse;
import com.parktech.model.EstadoTicket;
import com.parktech.model.TicketParqueo;
import com.parktech.model.TipoVehiculo;
import com.parktech.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Lógica del Consolidado Diario de Caja (RF-03 / HU-03).
 */
@Service
public class ReporteService {

    private final TicketRepository ticketRepository;

    public ReporteService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Genera el consolidado de un día.
     * @param fecha      día a consultar (si es null se usa hoy)
     * @param operarioId operario a filtrar (si es null se incluyen todos)
     */
    public ReporteCajaResponse consolidadoCaja(LocalDate fecha, Long operarioId) {
        LocalDate dia = (fecha != null) ? fecha : LocalDate.now();

        // Rango del día completo: 00:00:00 hasta 23:59:59.999...
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.atTime(LocalTime.MAX);

        List<TicketParqueo> tickets = (operarioId == null)
                ? ticketRepository.findByEstadoAndHoraSalidaBetweenOrderByHoraSalidaDesc(
                        EstadoTicket.SALIO, inicio, fin)
                : ticketRepository.findByEstadoAndOperarioIdAndHoraSalidaBetweenOrderByHoraSalidaDesc(
                        EstadoTicket.SALIO, operarioId, inicio, fin);

        long subtotalMotos = sumarPorTipo(tickets, TipoVehiculo.MOTO);
        long subtotalCarros = sumarPorTipo(tickets, TipoVehiculo.CARRO);

        return new ReporteCajaResponse(dia, operarioId,
                subtotalMotos + subtotalCarros, subtotalMotos, subtotalCarros,
                tickets.size(), tickets);
    }

    /** Suma el valor pagado de los tickets de un tipo de vehículo. */
    private long sumarPorTipo(List<TicketParqueo> tickets, TipoVehiculo tipo) {
        long suma = 0;
        for (TicketParqueo t : tickets) {
            if (t.getTipoVehiculo() == tipo && t.getValorPagado() != null) {
                suma += t.getValorPagado();
            }
        }
        return suma;
    }
}
