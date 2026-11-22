package com.parktech.service;

import com.parktech.dto.IngresoRequest;
import com.parktech.dto.LiquidacionResponse;
import com.parktech.exception.RecursoNoEncontradoException;
import com.parktech.model.*;
import com.parktech.repository.TarifaRepository;
import com.parktech.repository.TicketRepository;
import com.parktech.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lógica de negocio de los tickets de parqueo:
 *  - RF-01: registrar ingreso
 *  - RF-02: calcular tarifa y registrar salida
 *  - RN-01: cobro por hora completa desde el minuto 1
 *  - RN-02: no permitir placas duplicadas activas
 */
@Service
public class TicketService {

    /**
     * Formato clásico: 3 letras + 3 números (guion opcional). Ej: ABC123 o ABC-123.
     * Lo usan todos los carros y también las motos con placa del formato anterior.
     */
    private static final Pattern PLACA_TRES_NUMEROS = Pattern.compile("^[A-Z]{3}-?[0-9]{3}$");

    /** Formato moderno de moto: 3 letras + 2 números + 1 letra (guion opcional). Ej: ABC12D */
    private static final Pattern PLACA_MOTO_MODERNA = Pattern.compile("^[A-Z]{3}-?[0-9]{2}[A-Z]$");

    private final TicketRepository ticketRepository;
    private final TarifaRepository tarifaRepository;
    private final UsuarioRepository usuarioRepository;

    public TicketService(TicketRepository ticketRepository,
                         TarifaRepository tarifaRepository,
                         UsuarioRepository usuarioRepository) {
        this.ticketRepository = ticketRepository;
        this.tarifaRepository = tarifaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =====================================================================
    //  RF-01: Registrar ingreso
    // =====================================================================

    /**
     * Registra la entrada de un vehículo.
     * 1. Valida y normaliza la placa.
     * 2. Verifica que la placa no esté ya ADENTRO (RN-02).
     * 3. Guarda el ticket con la hora del servidor y el operario.
     */
    @Transactional
    public TicketParqueo registrarIngreso(IngresoRequest datos) {
        String placa = normalizarPlaca(datos.placa(), datos.tipoVehiculo());

        // RN-02: si ya hay un ticket ADENTRO con esa placa, se rechaza (409)
        if (ticketRepository.findByPlacaAndEstado(placa, EstadoTicket.ADENTRO).isPresent()) {
            throw new IllegalStateException("El vehículo ya se encuentra dentro del parqueadero");
        }

        Usuario operario = usuarioRepository.findById(datos.operarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El operario no existe"));

        TicketParqueo ticket = new TicketParqueo();
        ticket.setPlaca(placa);
        ticket.setTipoVehiculo(datos.tipoVehiculo());
        ticket.setHoraIngreso(LocalDateTime.now()); // hora exacta del servidor
        ticket.setEstado(EstadoTicket.ADENTRO);
        ticket.setOperario(operario);
        return ticketRepository.save(ticket);
    }

    /** Lista los vehículos que están actualmente dentro del parqueadero. */
    public List<TicketParqueo> listarVehiculosAdentro() {
        return ticketRepository.findByEstadoOrderByHoraIngresoDesc(EstadoTicket.ADENTRO);
    }

    // =====================================================================
    //  RF-02: Consultar liquidación y registrar salida
    // =====================================================================

    /**
     * Calcula cuánto debe pagar un vehículo que está adentro, sin cerrar el ticket.
     */
    public LiquidacionResponse consultarLiquidacion(String placaTexto) {
        TicketParqueo ticket = buscarTicketActivo(placaTexto);
        return liquidar(ticket, LocalDateTime.now());
    }

    /**
     * Confirma el pago: calcula el valor, cambia el estado a SALIO
     * y guarda la hora de salida y el valor pagado.
     */
    @Transactional
    public LiquidacionResponse registrarSalida(String placaTexto) {
        TicketParqueo ticket = buscarTicketActivo(placaTexto);
        LocalDateTime ahora = LocalDateTime.now();
        LiquidacionResponse liquidacion = liquidar(ticket, ahora);

        ticket.setHoraSalida(ahora);
        ticket.setValorPagado(liquidacion.totalAPagar());
        ticket.setEstado(EstadoTicket.SALIO);
        ticketRepository.save(ticket);

        return new LiquidacionResponse(
                ticket.getId(), ticket.getPlaca(), ticket.getTipoVehiculo(),
                ticket.getHoraIngreso(), ahora,
                liquidacion.minutosTranscurridos(), liquidacion.horasCobradas(),
                liquidacion.valorHora(), liquidacion.totalAPagar(), EstadoTicket.SALIO);
    }

    /** Busca el ticket ADENTRO de una placa; si no existe lanza 404. */
    private TicketParqueo buscarTicketActivo(String placaTexto) {
        String placa = normalizarPlaca(placaTexto, null);
        return ticketRepository.findByPlacaAndEstado(placa, EstadoTicket.ADENTRO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningún vehículo con placa " + placa + " dentro del parqueadero"));
    }

    /** Arma la liquidación de un ticket a una hora de corte dada. */
    private LiquidacionResponse liquidar(TicketParqueo ticket, LocalDateTime horaCorte) {
        Tarifa tarifa = tarifaRepository.findByTipoVehiculo(ticket.getTipoVehiculo())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay tarifa configurada para " + ticket.getTipoVehiculo()));

        long minutos = Duration.between(ticket.getHoraIngreso(), horaCorte).toMinutes();
        long horas = calcularHorasACobrar(minutos);
        long total = horas * tarifa.getValorHora();

        return new LiquidacionResponse(
                ticket.getId(), ticket.getPlaca(), ticket.getTipoVehiculo(),
                ticket.getHoraIngreso(), null,
                minutos, horas, tarifa.getValorHora(), total, ticket.getEstado());
    }

    // =====================================================================
    //  RN-01: Cobro por hora completa desde el minuto 1
    // =====================================================================

    /**
     * Convierte minutos en horas a cobrar, redondeando hacia arriba.
     * Ejemplos: 1 min -> 1 h | 59 min -> 1 h | 60 min -> 1 h | 61 min -> 2 h.
     * Si el vehículo sale antes del primer minuto (0 min) igual se cobra 1 hora.
     */
    public static long calcularHorasACobrar(long minutos) {
        long horas = (long) Math.ceil(minutos / 60.0);
        return Math.max(1, horas);
    }

    // =====================================================================
    //  Validación de placa (formato colombiano)
    // =====================================================================

    /**
     * Valida la placa y la devuelve normalizada en formato XXX-000 / XXX-00X.
     * @param tipo si no es null, verifica que el formato coincida con el tipo:
     *             CARRO = solo ABC-123;
     *             MOTO  = ABC-12D (formato moderno) o ABC-123 (formato anterior).
     */
    public static String normalizarPlaca(String placaTexto, TipoVehiculo tipo) {
        if (placaTexto == null || placaTexto.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        String placa = placaTexto.trim().toUpperCase();
        if (placa.contains(" ")) {
            throw new IllegalArgumentException("La placa no puede contener espacios");
        }

        boolean tieneTresNumeros = PLACA_TRES_NUMEROS.matcher(placa).matches();
        boolean esMotoModerna = PLACA_MOTO_MODERNA.matcher(placa).matches();

        if (!tieneTresNumeros && !esMotoModerna) {
            throw new IllegalArgumentException(
                    "Placa inválida. Use ABC-123 para carros, o ABC-12D / ABC-123 para motos");
        }
        // Un carro solo puede tener el formato de 3 números
        if (tipo == TipoVehiculo.CARRO && !tieneTresNumeros) {
            throw new IllegalArgumentException("La placa de un carro debe tener el formato ABC-123");
        }
        // Una moto acepta los dos formatos, que ya se validaron arriba:
        // ABC-12D (moderno) o ABC-123 (anterior), así que no necesita otra regla.

        // Si viene sin guion (ABC123) se lo agregamos después de las 3 letras
        String sinGuion = placa.replace("-", "");
        return sinGuion.substring(0, 3) + "-" + sinGuion.substring(3);
    }
}
