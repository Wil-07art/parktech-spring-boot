/* ============================================================
   ParkTech · Lógica del frontend (JavaScript Vanilla ES6+)
   - Login y sesión en sessionStorage
   - P-01 Panel de Ingreso
   - P-02 Panel de Salida y Cobro
   - P-03 Dashboard de Consolidado de Caja (solo ADMIN)
   ============================================================ */

const CLAVE_SESION = 'parktech_usuario';
let liquidacionActual = null;     // liquidación mostrada en el panel de salida
let reporteActual = null;         // último reporte de caja consultado (para el CSV)
let intervaloAdentro = null;      // refresco automático de la tabla de vehículos adentro

// ============================================================
//  Utilidades
// ============================================================

/** Atajo para document.getElementById */
const $ = (id) => document.getElementById(id);

/**
 * Llama a la API del backend y devuelve el JSON.
 * Si el servidor responde con error, lanza un Error con el "mensaje" del JSON.
 */
async function api(url, opciones = {}) {
    const respuesta = await fetch(url, {
        headers: { 'Content-Type': 'application/json' },
        ...opciones
    });
    const datos = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
        throw new Error(datos.mensaje || `Error ${respuesta.status}`);
    }
    return datos;
}

/** Muestra una notificación tipo toast. tipo: 'exito' | 'error' | 'advertencia' */
function toast(mensaje, tipo = 'exito') {
    const div = document.createElement('div');
    div.className = `toast toast-${tipo}`;
    div.textContent = mensaje;
    $('toasts').appendChild(div);
    setTimeout(() => div.remove(), 3500);
}

/** Formatea un número como pesos colombianos: 4000 -> $4.000 */
function pesos(valor) {
    return '$' + Number(valor || 0).toLocaleString('es-CO');
}

/** Convierte "2026-11-21T10:30:00.123456" en un objeto Date local. */
function aFecha(textoIso) {
    return new Date(String(textoIso).substring(0, 19));
}

/** Formatea fecha y hora: 21/11/2026 10:30 */
function fechaHora(textoIso) {
    if (!textoIso) return '—';
    return aFecha(textoIso).toLocaleString('es-CO', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: false
    });
}

/** Formatea minutos como "2 h 15 min" */
function duracion(minutos) {
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    return h > 0 ? `${h} h ${m} min` : `${m} min`;
}

/** Fecha de hoy en formato YYYY-MM-DD (hora local) */
function hoyIso() {
    return new Date().toLocaleDateString('en-CA');
}

/** Evita que un texto se interprete como HTML (seguridad básica). */
function escapar(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

/**
 * RNF-02: formatea la placa mientras se escribe.
 * Mayúsculas, sin espacios ni símbolos, y guion automático después de 3 letras.
 */
function formatearPlaca(valor) {
    const limpio = valor.toUpperCase().replace(/[^A-Z0-9]/g, '');
    return limpio.length > 3 ? limpio.slice(0, 3) + '-' + limpio.slice(3, 6) : limpio;
}

// ============================================================
//  Sesión y login
// ============================================================

function obtenerSesion() {
    const texto = sessionStorage.getItem(CLAVE_SESION);
    return texto ? JSON.parse(texto) : null;
}

/** Envía usuario y contraseña a POST /api/auth/login */
async function iniciarSesion(evento) {
    evento.preventDefault();
    try {
        const usuario = await api('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                username: $('login-usuario').value,
                password: $('login-password').value
            })
        });
        // Guardamos id, nombre y rol para el resto de la sesión del navegador
        sessionStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
        $('form-login').reset();
        mostrarApp();
        toast(`Bienvenido, ${usuario.nombre}`);
    } catch (error) {
        toast(error.message, 'error');
    }
}

function cerrarSesion() {
    sessionStorage.removeItem(CLAVE_SESION);
    clearInterval(intervaloAdentro);
    $('vista-app').classList.add('oculto');
    $('vista-login').classList.remove('oculto');
    $('login-usuario').focus();
}

/** Muestra la aplicación y aplica el control de roles (RNF-03). */
function mostrarApp() {
    const sesion = obtenerSesion();
    if (!sesion) return cerrarSesion();

    $('vista-login').classList.add('oculto');
    $('vista-app').classList.remove('oculto');
    $('usuario-nombre').textContent = sesion.nombre;
    $('usuario-rol').textContent = sesion.rol;

    // El OPERARIO no ve la pestaña de Consolidado de Caja
    const esAdmin = sesion.rol === 'ADMIN';
    document.querySelectorAll('.solo-admin').forEach(el => el.classList.toggle('oculto', !esAdmin));
    if (esAdmin) cargarOperarios();

    cambiarPanel('panel-ingreso');
    cargarVehiculosAdentro();
    clearInterval(intervaloAdentro);
    intervaloAdentro = setInterval(cargarVehiculosAdentro, 30000); // tabla en "tiempo real"
}

// ============================================================
//  Navegación entre paneles
// ============================================================

function cambiarPanel(idPanel) {
    // Protección extra: un operario no puede abrir el panel de caja
    if (idPanel === 'panel-caja' && obtenerSesion()?.rol !== 'ADMIN') return;

    document.querySelectorAll('.panel').forEach(p => p.classList.toggle('oculto', p.id !== idPanel));
    document.querySelectorAll('.tab').forEach(t => t.classList.toggle('activa', t.dataset.panel === idPanel));

    if (idPanel === 'panel-ingreso') $('ingreso-placa').focus();
    if (idPanel === 'panel-salida') $('salida-placa').focus();
    if (idPanel === 'panel-caja') consultarCaja();
}

// ============================================================
//  P-01 · Panel de Ingreso (HU-01)
// ============================================================

async function registrarIngreso(evento) {
    evento.preventDefault();
    const sesion = obtenerSesion();
    const tipo = document.querySelector('input[name="tipo"]:checked').value;
    try {
        const ticket = await api('/api/tickets/ingreso', {
            method: 'POST',
            body: JSON.stringify({ placa: $('ingreso-placa').value, tipoVehiculo: tipo, operarioId: sesion.id })
        });
        toast(`Ingreso registrado: ${ticket.placa}`);
        $('ingreso-placa').value = '';
        $('ingreso-placa').focus();
        cargarVehiculosAdentro();
    } catch (error) {
        // 409 (ya adentro) se muestra como advertencia; lo demás como error
        const tipoToast = error.message.includes('ya se encuentra') ? 'advertencia' : 'error';
        toast(error.message, tipoToast);
    }
}

/** Llena la tabla con GET /api/tickets/activos */
async function cargarVehiculosAdentro() {
    try {
        const tickets = await api('/api/tickets/activos');
        $('contador-adentro').textContent = tickets.length;
        const cuerpo = $('tabla-adentro');

        if (tickets.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="5" class="vacio">Sin vehículos adentro</td></tr>';
            return;
        }
        const ahora = new Date();
        cuerpo.innerHTML = tickets.map(t => {
            const minutos = Math.max(0, Math.floor((ahora - aFecha(t.horaIngreso)) / 60000));
            return `<tr>
                <td class="placa-celda">${escapar(t.placa)}</td>
                <td><span class="badge">${t.tipoVehiculo}</span></td>
                <td class="mono">${fechaHora(t.horaIngreso)}</td>
                <td class="mono">${duracion(minutos)}</td>
                <td>${escapar(t.operario?.nombre)}</td>
            </tr>`;
        }).join('');
    } catch (error) {
        toast('No se pudo cargar la lista de vehículos: ' + error.message, 'error');
    }
}

// ============================================================
//  P-02 · Panel de Salida y Cobro (HU-02)
// ============================================================

/** GET /api/tickets/consultar/{placa}: calcula el cobro sin cerrar el ticket */
async function consultarSalida(evento) {
    evento.preventDefault();
    const placa = $('salida-placa').value;
    try {
        liquidacionActual = await api(`/api/tickets/consultar/${encodeURIComponent(placa)}`);
        mostrarLiquidacion(liquidacionActual);
        $('btn-confirmar-pago').focus(); // Enter de nuevo confirma el pago
    } catch (error) {
        limpiarSalida();
        toast(error.message, 'error');
    }
}

function mostrarLiquidacion(liq) {
    $('liq-placa').textContent = liq.placa;
    $('liq-tipo').textContent = liq.tipoVehiculo;
    $('liq-ingreso').textContent = fechaHora(liq.horaIngreso);
    $('liq-tiempo').textContent = duracion(liq.minutosTranscurridos);
    $('liq-horas').textContent = `${liq.horasCobradas} h`;
    $('liq-valor-hora').textContent = pesos(liq.valorHora);
    $('liq-total').textContent = pesos(liq.totalAPagar);
    $('tarjeta-liquidacion').classList.remove('oculto');
    $('liquidacion-vacia').classList.add('oculto');
}

/** PUT /api/tickets/salida/{placa}: confirma el pago y cierra el ticket */
async function confirmarPago() {
    if (!liquidacionActual) return;
    const boton = $('btn-confirmar-pago');
    boton.disabled = true;
    try {
        const resultado = await api(`/api/tickets/salida/${encodeURIComponent(liquidacionActual.placa)}`, { method: 'PUT' });
        toast(`Pago confirmado: ${resultado.placa} · ${pesos(resultado.totalAPagar)}`);
        limpiarSalida();
        cargarVehiculosAdentro();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        boton.disabled = false;
    }
}

/** Limpieza automática del panel después de cobrar o cancelar */
function limpiarSalida() {
    liquidacionActual = null;
    $('salida-placa').value = '';
    $('tarjeta-liquidacion').classList.add('oculto');
    $('liquidacion-vacia').classList.remove('oculto');
    $('salida-placa').focus();
}

// ============================================================
//  P-03 · Dashboard Consolidado de Caja (HU-03, solo ADMIN)
// ============================================================

/** Llena el filtro de operarios con GET /api/usuarios */
async function cargarOperarios() {
    try {
        const usuarios = await api('/api/usuarios');
        const select = $('filtro-operario');
        select.innerHTML = '<option value="">Todos</option>' +
            usuarios.map(u => `<option value="${u.id}">${escapar(u.nombre)} (${u.rol})</option>`).join('');
    } catch (error) {
        toast('No se pudieron cargar los operarios', 'error');
    }
}

/** GET /api/reportes/caja?fecha=...&operarioId=... */
async function consultarCaja() {
    const fecha = $('filtro-fecha').value || hoyIso();
    const operarioId = $('filtro-operario').value;
    let url = `/api/reportes/caja?fecha=${fecha}`;
    if (operarioId) url += `&operarioId=${operarioId}`;

    try {
        reporteActual = await api(url);
        $('kpi-total').textContent = pesos(reporteActual.totalRecaudado);
        $('kpi-carros').textContent = pesos(reporteActual.subtotalCarros);
        $('kpi-motos').textContent = pesos(reporteActual.subtotalMotos);
        $('kpi-cantidad').textContent = reporteActual.cantidadTickets;
        $('titulo-fecha').textContent = fecha;

        const cuerpo = $('tabla-caja');
        if (reporteActual.tickets.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="vacio">Sin movimientos para esta fecha</td></tr>';
            return;
        }
        cuerpo.innerHTML = reporteActual.tickets.map(t => `<tr>
            <td class="mono">${t.id}</td>
            <td class="placa-celda">${escapar(t.placa)}</td>
            <td><span class="badge">${t.tipoVehiculo}</span></td>
            <td class="mono">${fechaHora(t.horaIngreso)}</td>
            <td class="mono">${fechaHora(t.horaSalida)}</td>
            <td>${escapar(t.operario?.nombre)}</td>
            <td class="mono der">${pesos(t.valorPagado)}</td>
        </tr>`).join('');
    } catch (error) {
        toast(error.message, 'error');
    }
}

/** Descarga los movimientos del reporte como archivo CSV */
function exportarCsv() {
    if (!reporteActual || reporteActual.tickets.length === 0) {
        toast('No hay movimientos para exportar', 'advertencia');
        return;
    }
    const filas = [['ID', 'Placa', 'Tipo', 'Hora ingreso', 'Hora salida', 'Operario', 'Valor pagado']];
    reporteActual.tickets.forEach(t => filas.push([
        t.id, t.placa, t.tipoVehiculo, fechaHora(t.horaIngreso), fechaHora(t.horaSalida),
        t.operario?.nombre ?? '', t.valorPagado
    ]));
    filas.push([]);
    filas.push(['', '', '', '', '', 'Subtotal motos', reporteActual.subtotalMotos]);
    filas.push(['', '', '', '', '', 'Subtotal carros', reporteActual.subtotalCarros]);
    filas.push(['', '', '', '', '', 'TOTAL', reporteActual.totalRecaudado]);

    // Punto y coma como separador (Excel en español) y comillas para cada celda
    const csv = filas.map(f => f.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(';')).join('\r\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' }); // BOM para tildes
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(blob);
    enlace.download = `cierre-caja-${reporteActual.fecha}.csv`;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
}

// ============================================================
//  Reloj y eventos
// ============================================================

function actualizarReloj() {
    $('reloj').textContent = new Date().toLocaleTimeString('es-CO', { hour12: false });
}

document.addEventListener('DOMContentLoaded', () => {
    // Login / logout
    $('form-login').addEventListener('submit', iniciarSesion);
    $('btn-salir').addEventListener('click', cerrarSesion);

    // Pestañas
    document.querySelectorAll('.tab').forEach(tab =>
        tab.addEventListener('click', () => cambiarPanel(tab.dataset.panel)));

    // Placas en mayúsculas automáticas
    ['ingreso-placa', 'salida-placa'].forEach(id =>
        $(id).addEventListener('input', e => { e.target.value = formatearPlaca(e.target.value); }));

    // Formularios (Enter funciona automáticamente al ser <form>)
    $('form-ingreso').addEventListener('submit', registrarIngreso);
    $('form-consulta').addEventListener('submit', consultarSalida);
    $('btn-confirmar-pago').addEventListener('click', confirmarPago);
    $('btn-cancelar-cobro').addEventListener('click', limpiarSalida);

    // Dashboard de caja
    $('filtro-fecha').value = hoyIso();
    $('btn-consultar-caja').addEventListener('click', consultarCaja);
    $('filtro-fecha').addEventListener('change', consultarCaja);
    $('filtro-operario').addEventListener('change', consultarCaja);
    $('btn-exportar-csv').addEventListener('click', exportarCsv);
    $('btn-imprimir').addEventListener('click', () => window.print());

    // Reloj
    actualizarReloj();
    setInterval(actualizarReloj, 1000);

    // Si ya había sesión abierta (recarga de página), entra directo
    if (obtenerSesion()) {
        mostrarApp();
    } else {
        $('login-usuario').focus();
    }
});
