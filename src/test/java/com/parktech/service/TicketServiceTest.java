package com.parktech.service;

import com.parktech.dto.IngresoRequest;
import com.parktech.model.EstadoTicket;
import com.parktech.model.Rol;
import com.parktech.model.TicketParqueo;
import com.parktech.model.TipoVehiculo;
import com.parktech.model.Usuario;
import com.parktech.repository.TarifaRepository;
import com.parktech.repository.TicketRepository;
import com.parktech.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de TicketService con JUnit 5.
 * No necesitan MySQL: los repositorios se reemplazan por "mocks" (objetos falsos)
 * creados con Mockito, que viene incluido en spring-boot-starter-test.
 */
class TicketServiceTest {

    // ---------------- RN-01: cobro por hora completa ----------------

    @Test
    void unMinutoSeCobraComoUnaHora() {
        assertEquals(1, TicketService.calcularHorasACobrar(1));
    }

    @Test
    void cincuentaYNueveMinutosSeCobranComoUnaHora() {
        assertEquals(1, TicketService.calcularHorasACobrar(59));
    }

    @Test
    void sesentaMinutosSeCobranComoUnaHora() {
        assertEquals(1, TicketService.calcularHorasACobrar(60));
    }

    @Test
    void sesentaYUnMinutosSeCobranComoDosHoras() {
        assertEquals(2, TicketService.calcularHorasACobrar(61));
    }

    @Test
    void ceroMinutosTambienSeCobraUnaHora() {
        assertEquals(1, TicketService.calcularHorasACobrar(0));
    }

    // ---------------- RN-02: sin placas duplicadas activas ----------------

    @Test
    void noPermiteIngresarPlacaQueYaEstaAdentro() {
        // Preparar: repositorios falsos
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TarifaRepository tarifaRepository = mock(TarifaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        TicketService servicio = new TicketService(ticketRepository, tarifaRepository, usuarioRepository);

        // Simular que ABC-123 ya tiene un ticket ADENTRO
        when(ticketRepository.findByPlacaAndEstado("ABC-123", EstadoTicket.ADENTRO))
                .thenReturn(Optional.of(new TicketParqueo()));

        // Ejecutar y verificar: debe lanzar IllegalStateException (HTTP 409)
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.registrarIngreso(new IngresoRequest("abc123", TipoVehiculo.CARRO, 1L)));

        assertEquals("El vehículo ya se encuentra dentro del parqueadero", error.getMessage());
        // Y nunca debe haber intentado guardar un ticket nuevo
        verify(ticketRepository, never()).save(any());
    }

    // ---------------- Validación de placa ----------------

    @Test
    void normalizaPlacaDeCarroAgregandoGuion() {
        assertEquals("ABC-123", TicketService.normalizarPlaca("abc123", TipoVehiculo.CARRO));
    }

    @Test
    void normalizaPlacaDeMoto() {
        assertEquals("XYZ-12D", TicketService.normalizarPlaca("XYZ-12D", TipoVehiculo.MOTO));
    }

    @Test
    void aceptaPlacaDeMotoConFormatoAnterior() {
        assertEquals("XYZ-456", TicketService.normalizarPlaca("xyz456", TipoVehiculo.MOTO));
    }

    @Test
    void motoConPlacaXYZ456IngresaCorrectamente() {
        // Preparar: repositorios falsos
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TarifaRepository tarifaRepository = mock(TarifaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        TicketService servicio = new TicketService(ticketRepository, tarifaRepository, usuarioRepository);

        Usuario operario = new Usuario(2L, "Operario 1", "operario1", "oper123", Rol.OPERARIO);
        // La placa no está adentro, el operario existe y save() devuelve el mismo ticket
        when(ticketRepository.findByPlacaAndEstado("XYZ-456", EstadoTicket.ADENTRO)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(operario));
        when(ticketRepository.save(any(TicketParqueo.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ejecutar
        TicketParqueo ticket = servicio.registrarIngreso(new IngresoRequest("XYZ-456", TipoVehiculo.MOTO, 2L));

        // Verificar
        assertEquals("XYZ-456", ticket.getPlaca());
        assertEquals(TipoVehiculo.MOTO, ticket.getTipoVehiculo());
        assertEquals(EstadoTicket.ADENTRO, ticket.getEstado());
        assertEquals(operario, ticket.getOperario());
        assertNotNull(ticket.getHoraIngreso());
    }

    @Test
    void rechazaPlacaConFormatoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> TicketService.normalizarPlaca("AB1234", TipoVehiculo.CARRO));
    }

    @Test
    void rechazaPlacaDeMotoRegistradaComoCarro() {
        assertThrows(IllegalArgumentException.class,
                () -> TicketService.normalizarPlaca("ABC12D", TipoVehiculo.CARRO));
    }
}
