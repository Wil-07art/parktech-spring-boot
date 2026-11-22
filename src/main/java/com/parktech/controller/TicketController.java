package com.parktech.controller;

import com.parktech.dto.IngresoRequest;
import com.parktech.dto.LiquidacionResponse;
import com.parktech.model.TicketParqueo;
import com.parktech.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de ingreso y salida de vehículos (HU-01 y HU-02).
 * El controlador solo recibe la petición y delega al servicio;
 * los errores los convierte en JSON el GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** POST /api/tickets/ingreso -> 201 Created | 400 placa inválida | 409 placa ya adentro */
    @PostMapping("/ingreso")
    public ResponseEntity<TicketParqueo> registrarIngreso(@Valid @RequestBody IngresoRequest datos) {
        TicketParqueo ticket = ticketService.registrarIngreso(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /** GET /api/tickets/activos -> vehículos que están ADENTRO (tabla del Panel de Ingreso) */
    @GetMapping("/activos")
    public List<TicketParqueo> listarActivos() {
        return ticketService.listarVehiculosAdentro();
    }

    /** GET /api/tickets/consultar/{placa} -> liquidación sin cobrar | 404 si no está adentro */
    @GetMapping("/consultar/{placa}")
    public LiquidacionResponse consultar(@PathVariable String placa) {
        return ticketService.consultarLiquidacion(placa);
    }

    /** PUT /api/tickets/salida/{placa} -> confirma el pago y cierra el ticket */
    @PutMapping("/salida/{placa}")
    public LiquidacionResponse registrarSalida(@PathVariable String placa) {
        return ticketService.registrarSalida(placa);
    }
}
