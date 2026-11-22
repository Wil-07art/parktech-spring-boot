# ParkTech · Gestión de Parqueadero

Proyecto académico (4.º semestre de Ingeniería de Software) con **Spring Boot 3, MySQL 8 y HTML/CSS/JS puro**.
Implementa las épicas EP-01 y EP-02: ingreso de vehículos, salida con cobro y consolidado diario de caja.

## Requisitos
- Java 17 o superior
- Maven 3.9+
- MySQL 8 en `localhost:3306`

## Puesta en marcha
1. Crear la base de datos:
   ```sql
   DROP DATABASE IF EXISTS parktech_db;
   CREATE DATABASE parktech_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Poner la contraseña de MySQL en `src/main/resources/application.properties`
   (`spring.datasource.password`) o en la variable de entorno `MYSQL_PASSWORD`.
3. Ejecutar las pruebas: `mvn clean test`
4. Levantar el servidor: `mvn spring-boot:run`
5. Abrir http://localhost:8080

## Usuarios de prueba
| Usuario     | Contraseña | Rol      |
|-------------|------------|----------|
| `admin`     | `admin123` | ADMIN    |
| `operario1` | `oper123`  | OPERARIO |

Tarifas iniciales: MOTO $2.000/hora · CARRO $4.000/hora.

## Arquitectura (4 capas)
```
com.parktech
├── model        Entidades JPA (Usuario, Tarifa, TicketParqueo) y enums
├── repository   Interfaces Spring Data JPA
├── service      Reglas de negocio (RN-01, RN-02, validación de placa, reportes)
├── controller   Endpoints REST
├── dto          Objetos simples de entrada/salida (records)
├── exception    Manejo global de errores -> { "mensaje": "..." }
└── config       DataInitializer (datos iniciales)
```

## Endpoints
| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/auth/login` | Login (200 / 401) |
| POST | `/api/tickets/ingreso` | Registrar ingreso (201 / 400 / 409) |
| GET  | `/api/tickets/activos` | Vehículos adentro |
| GET  | `/api/tickets/consultar/{placa}` | Liquidación sin cobrar (200 / 404) |
| PUT  | `/api/tickets/salida/{placa}` | Confirmar pago (200 / 404) |
| GET  | `/api/reportes/caja?fecha=YYYY-MM-DD&operarioId=` | Consolidado de caja |
| GET  | `/api/usuarios` | Usuarios para el filtro de operario |

## Reglas de negocio
- **RN-01:** toda fracción de hora se cobra completa: `Math.ceil(minutos / 60.0)` (mínimo 1 hora).
- **RN-02:** una placa con ticket `ADENTRO` no puede volver a ingresar (HTTP 409).
- **Placa:** carro solo `ABC-123`; moto `ABC-12D` (formato moderno) o `ABC-123` (formato anterior); se normaliza siempre con guion.

