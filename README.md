# TicketFast API

Backend de alto rendimiento para la gestión de eventos, sedes, reservas temporales de asientos de alta concurrencia y venta de entradas. Implementa una **arquitectura híbrida NoSQL** combinando la persistencia de documentos en **MongoDB** con la velocidad y bloqueos atómicos en memoria de **Redis**.

---

## 📋 Tabla de Contenidos

1. [Descripción y Propósito](#-descripción-y-propósito)
2. [Arquitectura y Stack Tecnológico](#-arquitectura-y-stack-tecnológico)
3. [Modelo de Datos NoSQL](#-modelo-de-datos-nosql)
   - [Persistencia en MongoDB](#persistencia-en-mongodb)
   - [Bloqueos y Concurrencia en Redis](#bloqueos-y-concurrencia-en-redis)
4. [Seguridad y Control de Acceso (RBAC + JWT)](#-seguridad-y-control-de-acceso-rbac--jwt)
5. [Requisitos Previos y Variables de Entorno](#-requisitos-previos-y-variables-de-entorno)
6. [Instalación, Compilación y Ejecución](#-instalación-compilación-y-ejecución)
7. [Datos Precargados (Data Seeding)](#-datos-precargados-data-seeding)
8. [Documentación Interactiva (Swagger UI)](#-documentación-interactiva-swagger-ui)
9. [Catálogo de Endpoints REST](#-catálogo-de-endpoints-rest)
10. [Flujo Completo de Compra (Paso a Paso)](#-flujo-completo-de-compra-paso-a-paso)
11. [Estructura del Proyecto](#-estructura-del-proyecto)
12. [Manejo de Errores](#-manejo-de-errores)

---

## 🎯 Descripción y Propósito

**TicketFast API** resuelve el problema crítico de la venta masiva de entradas: la prevención estricta del **overbooking** (sobreventa de asientos) en momentos de alta concurrencia.

### Principales Capacidades:
- **Gestión desacoplada:** Separación de recintos físicos (`Sedes`), carteleras (`Eventos`) y asignación de precios por fecha (`EventoEntrada`).
- **Bloqueo temporal de asientos (10 minutos):** Reservas atómicas en memoria respaldadas por Redis con tiempo de expiración automático (TTL).
- **Liberación segura de reservas:** Uso de scripts atómicos en Lua que validan el token criptográfico del propietario antes de liberar o reasignar un asiento.
- **Órdenes de compra e idempotencia:** Procesamiento controlado de pagos simulados (aprobaciones/rechazos) y prevención de cobros duplicados.
- **Emisión permanente de tickets:** Generación de identificadores de ticket únicos tras la aprobación del pago.

---

## 🚀 Arquitectura y Stack Tecnológico

- **Lenguaje:** Java 21 (LTS)
- **Framework Principal:** Spring Boot 3.5.x
- **Bases de Datos NoSQL:**
  - **MongoDB:** Base de datos principal de documentos (`ticketfast_db`).
  - **Redis:** Almacén en memoria de clave-valor para bloqueos distribuidos y TTL.
- **Seguridad:** Spring Security 6 + JSON Web Tokens (JJWT) con firmado HMAC-SHA256.
- **Documentación de API:** Springdoc OpenAPI v2 / Swagger UI 3.
- **Herramientas de Productividad:** Lombok, Jakarta Bean Validation.
- **Gestor de Construcción:** Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## 🗄️ Modelo de Datos NoSQL

### Persistencia en MongoDB

El modelo de MongoDB aprovecha la flexibilidad del esquema y los documentos embebidos para representar recintos y operaciones complejas:

```
┌──────────────┐         ┌────────────────────┐         ┌──────────────┐
│    Sede      │         │   EventoEntrada    │         │    Evento    │
│  (Zonas &    │◄────────┤ (Precios por zona) ├────────►│  (Cartelera  │
│   Asientos)  │         └────────────────────┘         │    y tipo)   │
└──────────────┘                                        └──────────────┘
                               ▲
                               │
               ┌───────────────┴───────────────┐
               │         OrdenCompra           │
               │  (Importe, Asiento y Estado)  │
               └───────────────┬───────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
         ┌──────────────┐              ┌──────────────┐
         │     Pago     │              │   Entrada    │
         │  (Simulado)  │              │ (Definitiva) │
         └──────────────┘              └──────────────┘
```

#### Colecciones:
1. **`usuarios`**: Almacena datos personales, credenciales encriptadas con BCrypt y roles (`ROLE_CLIENTE`, `ROLE_ADMIN`).
2. **`sedes`**: Documento jerárquico que contiene una lista embebida de `Zona` y, dentro de cada zona, una lista embebida de `Asiento` (código, fila, número).
3. **`eventos`**: Define nombre, descripción, fecha, tipo de evento y estado (`ACTIVO`, `CANCELADO`, `FINALIZADO`). Contiene un campo `lugar` de texto descriptivo.
4. **`evento_entradas`**: Entidad puente que asocia un `eventoId` con un `sedeId` y define la lista de precios de venta (`precioVenta`) para cada código de zona.
5. **`ordenes_compra`**: Registro de la intención de compra con estado (`PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA`), usuario, evento, sede, asiento e importe calculado.
6. **`pagos`**: Registro de transacciones con referencia única, método de pago, monto, estado e idempotencia.
7. **`entradas`**: Ticket definitivo emitido post-pago con `codigoTicket` único, estado (`EMITIDA`, `USADA`, `ANULADA`) y fecha de emisión.

---

### Bloqueos y Concurrencia en Redis

- **Estructura de Clave:**
  ```
  ticketfast:reserva:<Base64(eventoId:sedeId:codigoZona:codigoAsiento)>
  ```
- **Valor:** UUID aleatorio generado como token de reserva (ej. `d3b07384-d113-4e42-9988-123456789abc`).
- **Comportamiento Atómico:**
  - **Bloqueo (`SET NX EX`):** Se ejecuta `setIfAbsent` con un TTL de 600 segundos (10 minutos). Si el asiento ya está bloqueado, Redis rechaza la operación inmediatamente.
  - **Liberación Segura (Lua Script):**
    ```lua
    if redis.call('get', KEYS[1]) == ARGV[1] then
        return redis.call('del', KEYS[1])
    else
        return 0
    end
    ```
    Garantiza que una reserva expirada o reasignada no pueda ser eliminada accidentalmente por otro usuario.

---

## 🔐 Seguridad y Control de Acceso (RBAC + JWT)

El sistema utiliza una arquitectura de seguridad **Stateless** basada en Spring Security y tokens JWT:

### Roles del Sistema:
- **`ROLE_CLIENTE`:** Puede reservar asientos, generar órdenes de compra, procesar pagos y consultar sus propias entradas/perfil.
- **`ROLE_ADMIN`:** Acceso total. Incluye la creación y mantenimiento de sedes, eventos, configuración de precios de entradas y consulta global de usuarios.

### Matriz de Acceso:

| Módulo / Recurso | Método HTTP | Ruta | Acceso Requerido |
| :--- | :--- | :--- | :--- |
| **Autenticación** | `POST` | `/api/auth/registro`, `/api/auth/login`, `/api/auth/logout` | **Público** |
| **Perfil Usuario** | `GET` | `/api/auth/me` | Autenticado |
| **Salud Conexiones** | `GET` | `/api/health/conexiones` | **Público** |
| **Catálogo Eventos** | `GET` | `/api/eventos`, `/api/eventos/{id}` | **Público** |
| **Gestión Eventos** | `POST`, `PUT`, `DELETE` | `/api/eventos/**` | `ROLE_ADMIN` |
| **Consulta Sedes** | `GET` | `/api/sedes`, `/api/sedes/{id}` | **Público** |
| **Gestión Sedes** | `POST`, `DELETE` | `/api/sedes/**` | `ROLE_ADMIN` |
| **Config. Precios** | `GET` | `/api/evento-entradas/**` | **Público** |
| **Gestión Precios** | `POST` | `/api/evento-entradas` | `ROLE_ADMIN` |
| **Reservas** | `POST`, `DELETE` | `/api/reservas/**` | Autenticado |
| **Órdenes de Compra**| `POST`, `GET` | `/api/ordenes/**` | Autenticado |
| **Pagos** | `POST`, `GET` | `/api/pagos/**` | Autenticado |
| **Entradas** | `GET` | `/api/entradas/**` | Autenticado |
| **Admin Usuarios** | `GET` | `/api/usuarios/**` | `ROLE_ADMIN` |

---

## ⚙️ Requisitos Previos y Variables de Entorno

### Requisitos del Sistema:
- **Java Development Kit (JDK):** Versión 21 o superior.
- **MongoDB:** Instancia activa (local en puerto `27017` o remota en MongoDB Atlas).
- **Redis:** Instancia activa (local en puerto `6379` o remota en Redis Cloud / Upstash).

### Variables de Entorno (`application.yaml`):

| Variable | Descripción | Valor de Ejemplo / Local |
| :--- | :--- | :--- |
| `MONGODB_URI` | Cadena de conexión a MongoDB | `mongodb://localhost:27017/ticketfast_db` |
| `REDIS_URL` | URL de conexión a Redis | `redis://localhost:6379` |
| `SERVER_PORT` | (Opcional) Puerto del servidor web | `8080` (por defecto) |

---

## 🛠️ Instalación, Compilación y Ejecución

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/ticketfast-api.git
cd ticketfast-api
```

### 2. Configurar las variables de entorno

**En Windows (PowerShell):**
```powershell
$env:MONGODB_URI="mongodb://localhost:27017/ticketfast_db"
$env:REDIS_URL="redis://localhost:6379"
```

**En Linux / macOS (Bash):**
```bash
export MONGODB_URI="mongodb://localhost:27017/ticketfast_db"
export REDIS_URL="redis://localhost:6379"
```

### 3. Ejecutar la suite de pruebas unitarias y de integración
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 4. Iniciar la aplicación
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

La aplicación iniciará en `http://localhost:8080`.

---

## 📦 Datos Precargados (Data Seeding)

Al iniciar el backend por primera vez, el componente `DataInitializer` verifica y precarga automáticamente la base de datos con las siguientes credenciales y entidades de prueba para agilizar la evaluación:

### 👤 Usuarios de Prueba:
- **Administrador:**
  - **Correo:** `admin@ticketfast.com`
  - **Contraseña:** `Admin123!`
  - **Rol:** `ROLE_ADMIN`
- **Cliente:**
  - **Correo:** `cliente@ticketfast.com`
  - **Contraseña:** `Cliente123!`
  - **Rol:** `ROLE_CLIENTE`

### 🏟️ Sede y Evento Precargados:
- **Sede:** `Estadio Nacional` (Lima) con zonas `VIP` ($80.00 base, asientos `VIP-A1`, `VIP-A2`) y `GENERAL` ($80.00 base, asientos `GEN-B1`, `GEN-B2`).
- **Evento:** `Concierto de Rock 2026` (`CONCIERTO`, estado `ACTIVO`).
- **Precios de Venta Configurados:** `VIP` a **$200.00** y `GENERAL` a **$100.00**.

---

## 📖 Documentación Interactiva (Swagger UI)

La API cuenta con documentación interactiva completa generada con OpenAPI:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

### Cómo autenticarse en Swagger UI:
1. Dirígete a la sección **`auth-controller`** y ejecuta `POST /api/auth/login` con las credenciales deseadas.
2. Copia el token recibido en el campo `token` de la respuesta JSON.
3. Haz clic en el botón verde **Authorize** (arriba a la derecha en Swagger).
4. Pega el token y presiona **Authorize**. Los endpoints protegidos quedarán habilitados para su ejecución.

---

## 📡 Catálogo de Endpoints REST

### 1. Autenticación (`/api/auth`)
- `POST /api/auth/registro` - Registro público de nuevos clientes.
- `POST /api/auth/login` - Inicio de sesión y obtención de token JWT.
- `POST /api/auth/logout` - Cierre de sesión del cliente.
- `GET /api/auth/me` - Consulta del perfil del usuario autenticado.

### 2. Monitoreo y Diagnóstico (`/api/health`)
- `GET /api/health/conexiones` - Diagnóstico en tiempo real del estado de MongoDB y Redis.

### 3. Sedes (`/api/sedes`)
- `GET /api/sedes` - Listar todas las sedes con sus zonas y asientos.
- `GET /api/sedes/{id}` - Obtener el detalle de una sede por ID.
- `POST /api/sedes` - Crear nueva sede con estructura física `[ROLE_ADMIN]`.
- `DELETE /api/sedes/{id}` - Eliminar sede por ID `[ROLE_ADMIN]`.

### 4. Eventos (`/api/eventos`)
- `GET /api/eventos` - Listar todos los eventos.
- `GET /api/eventos/{id}` - Obtener evento por ID.
- `POST /api/eventos` - Crear nuevo evento `[ROLE_ADMIN]`.
- `PUT /api/eventos/{id}` - Actualizar evento existente `[ROLE_ADMIN]`.
- `DELETE /api/eventos/{id}` - Eliminar evento por ID `[ROLE_ADMIN]`.

### 5. Configuración de Entradas (`/api/evento-entradas`)
- `GET /api/evento-entradas` - Listar todas las configuraciones de venta.
- `GET /api/evento-entradas/{id}` - Obtener configuración por ID.
- `GET /api/evento-entradas/evento/{eventoId}` - Consultar precios y zonas para un evento específico.
- `POST /api/evento-entradas` - Habilitar zonas y asignar precios a un evento `[ROLE_ADMIN]`.

### 6. Reservas Temporales (`/api/reservas`)
- `POST /api/reservas` - Bloquear temporalmente un asiento en Redis (10 min).
- `DELETE /api/reservas` - Liberar reserva temporal mediante token de reserva.

### 7. Órdenes de Compra (`/api/ordenes`)
- `POST /api/ordenes` - Crear orden de compra validando token de reserva activo.
- `GET /api/ordenes` - Listar órdenes de compra.
- `GET /api/ordenes/{id}` - Consultar orden por ID.
- `GET /api/ordenes/usuario/{usuarioId}` - Listar órdenes de un usuario.
- `GET /api/ordenes/evento/{eventoId}` - Listar órdenes de un evento.

### 8. Pagos (`/api/pagos`)
- `POST /api/pagos` - Procesar pago simulado con control de idempotencia y emitir entrada.
- `GET /api/pagos/{id}` - Consultar pago por ID.
- `GET /api/pagos/orden/{ordenCompraId}` - Consultar pago asociado a una orden.
- `GET /api/pagos/referencia/{referenciaTransaccion}` - Consultar pago por referencia externa.

### 9. Entradas (`/api/entradas`)
- `GET /api/entradas` - Listar todas las entradas emitidas.
- `GET /api/entradas/{id}` - Consultar entrada por ID.
- `GET /api/entradas/codigo/{codigoTicket}` - Consultar entrada por código único de ticket.
- `GET /api/entradas/usuario/{usuarioId}` - Listar entradas de un usuario.
- `GET /api/entradas/evento/{eventoId}` - Listar entradas vendidas para un evento.

### 10. Administración de Usuarios (`/api/usuarios`)
- `GET /api/usuarios` - Listar todos los usuarios registrados `[ROLE_ADMIN]`.
- `GET /api/usuarios/{id}` - Consultar usuario por ID `[ROLE_ADMIN]`.

---

## 🔄 Flujo Completo de Compra (Paso a Paso)

A continuación se describe la secuencia transaccional completa para adquirir una entrada:

```
[1. Iniciar Sesión] ────► [2. Bloquear Asiento] ────► [3. Crear Orden] ────► [4. Procesar Pago] ────► [5. Entrada Emitida]
   POST /api/auth/login      POST /api/reservas          POST /api/ordenes       POST /api/pagos        GET /api/entradas/usuario/{id}
   (Obtiene Bearer JWT)      (Obtiene Token UUID)        (Estado: PENDIENTE)     (Simulado / Aprobado)   (Ticket Definitivo)
```

1. **Autenticación (`POST /api/auth/login`):**
   Envía las credenciales y obtén el `token` Bearer.
2. **Reserva en Redis (`POST /api/reservas`):**
   Envía `eventoId`, `sedeId`, `codigoZona`, `codigoAsiento` y `usuarioId`. Recibirás un `token` de reserva con vigencia de 10 minutos.
3. **Creación de Orden (`POST /api/ordenes`):**
   Envía los datos del asiento junto con el `tokenReserva`. El sistema valida la vigencia en Redis, calcula el importe según la configuración de la zona y crea la orden en estado `PENDIENTE`.
4. **Pago Simulado (`POST /api/pagos`):**
   Envía `ordenCompraId`, `metodoPago` (ej. `TARJETA_CREDITO`), `monto`, `referenciaTransaccion` (ID único para idempotencia) y `simularAprobacion: true`.
   - Si es aprobado: La orden pasa a `APROBADA`, se libera el bloqueo en Redis y se genera automáticamente la **`Entrada`** definitiva en MongoDB.
5. **Consulta de Entrada (`GET /api/entradas/usuario/{usuarioId}`):**
   El cliente visualiza sus entradas adquiridas con su respectivo `codigoTicket`.

---

## 📂 Estructura del Proyecto

```
ticketfast-api/
├── pom.xml                               # Configuración de dependencias Maven
├── HELP.md
├── README.md                             # Documentación general del proyecto
└── src/
    ├── main/
    │   ├── java/com/ticketfastevents/api/
    │   │   ├── config/                   # Configuración OpenAPI y DataInitializer
    │   │   ├── controller/               # Controladores REST
    │   │   ├── dto/                      # Records y DTOs de petición y respuesta con validaciones
    │   │   ├── exception/                # GlobalExceptionHandler para respuestas de error
    │   │   ├── model/                    # Modelos de dominio y documentos MongoDB
    │   │   ├── repository/               # Repositorios Spring Data MongoDB
    │   │   ├── security/                 # Filtros JWT, UserPrincipal, SecurityConfig
    │   │   ├── service/                  # Lógica de negocio y operaciones atómicas en Redis
    │   │   └── TicketfastApiApplication.java
    │   └── resources/
    │       └── application.yaml          # Configuración de entorno y puertos
    └── test/
        ├── java/com/ticketfastevents/api/
        │   ├── controller/               # Pruebas unitarias de controladores WebMvc
        │   ├── security/                 # Pruebas de integración de seguridad y JWT
        │   ├── service/                  # Pruebas unitarias de servicios de negocio
        │   └── TicketfastApiApplicationTests.java
        └── resources/
            └── application.yaml          # Configuración de pruebas
```

---

## ⚠️ Manejo de Errores

La API utiliza un controlador de excepciones global (`GlobalExceptionHandler`) que formatea las respuestas de error en un formato JSON estándar y descriptivo:

```json
{
  "timestamp": "2026-10-25T18:00:00Z",
  "status": 409,
  "error": "409 CONFLICT",
  "message": "El asiento ya se encuentra reservado temporalmente por otro usuario"
}
```

### Códigos de Estado Comunes:
- `200 OK` / `201 Created` - Solicitud exitosa.
- `400 Bad Request` - Parámetros de validación faltantes o incorrectos.
- `401 Unauthorized` - Token JWT ausente, expirado o con firma inválida.
- `403 Forbidden` - Permisos insuficientes (se requiere `ROLE_ADMIN`).
- `404 Not Found` - Recurso no encontrado (usuario, evento, sede, orden o pago inexistente).
- `409 Conflict` - Conflicto de negocio (asiento ya reservado, token de reserva inválido/expirado, o entrada ya vendida).
