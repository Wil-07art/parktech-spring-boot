package com.parktech.controller;

import com.parktech.dto.ReporteCajaResponse;
import com.parktech.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Endpoint del Consolidado Diario de Caja (HU-03).
 * GET /api/reportes/caja?fecha=2026-11-21&operarioId=2
 * Ambos parámetros son opcionales: sin fecha usa hoy, sin operario trae todos.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/caja")
    public ReporteCajaResponse consolidadoCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long operarioId) {
        return reporteService.consolidadoCaja(fecha, operarioId);
    }
}
