package com.parktech.repository;

import com.parktech.model.EstadoTicket;
import com.parktech.model.TicketParqueo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la tabla "tickets_parqueo".
 * Todas las consultas son "derivadas": Spring lee el nombre del método y arma el SQL.
 */
public interface TicketRepository extends JpaRepository<TicketParqueo, Long> {

    /**
     * Busca el ticket de una placa con un estado dado.
     * Se usa para RN-02 (placa ya ADENTRO) y para encontrar el vehículo que va a salir.
     * SQL: SELECT * FROM tickets_parqueo WHERE placa = ? AND estado = ?
     */
    Optional<TicketParqueo> findByPlacaAndEstado(String placa, EstadoTicket estado);

    /**
     * Lista los vehículos con un estado, del más reciente al más antiguo.
     * Se usa para la tabla en tiempo real del Panel de Ingreso.
     */
    List<TicketParqueo> findByEstadoOrderByHoraIngresoDesc(EstadoTicket estado);

    /**
     * Tickets cerrados en un rango de horas de salida (un día completo).
     * Se usa para el consolidado de caja sin filtro de operario.
     */
    List<TicketParqueo> findByEstadoAndHoraSalidaBetweenOrderByHoraSalidaDesc(
            EstadoTicket estado, LocalDateTime inicio, LocalDateTime fin);

    /**
     * Igual que el anterior pero filtrando por el id del operario.
     * "OperarioId" significa: propiedad operario -> su campo id.
     */
    List<TicketParqueo> findByEstadoAndOperarioIdAndHoraSalidaBetweenOrderByHoraSalidaDesc(
            EstadoTicket estado, Long operarioId, LocalDateTime inicio, LocalDateTime fin);
}
