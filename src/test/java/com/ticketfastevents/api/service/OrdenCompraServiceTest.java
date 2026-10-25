package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.OrdenCompraRequest;
import com.ticketfastevents.api.dto.OrdenCompraResponse;
import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.model.EventoEntrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.EventoEntradaRepository;
import com.ticketfastevents.api.repository.EventoRepository;
import com.ticketfastevents.api.repository.OrdenCompraRepository;
import com.ticketfastevents.api.repository.SedeRepository;
import com.ticketfastevents.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private EventoEntradaRepository eventoEntradaRepository;

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private OrdenCompraService ordenCompraService;

    private Usuario usuario;
    private Evento evento;
    private Sede sede;
    private EventoEntrada eventoEntrada;
    private OrdenCompraRequest requestValido;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id("user-123")
                .nombres("Juan")
                .apellidos("Perez")
                .correo("juan@example.com")
                .activo(true)
                .build();

        evento = Evento.builder()
                .id("evt-123")
                .nombre("Concierto Rock")
                .estado(Evento.EstadoEvento.ACTIVO)
                .build();

        Sede.Asiento asiento = Sede.Asiento.builder()
                .codigo("A1")
                .fila("A")
                .numero(1)
                .build();

        Sede.Zona zona = Sede.Zona.builder()
                .codigo("VIP")
                .nombre("Zona VIP")
                .precioBase(BigDecimal.valueOf(100))
                .asientos(List.of(asiento))
                .build();

        sede = Sede.builder()
                .id("sede-123")
                .nombre("Estadio Nacional")
                .zonas(List.of(zona))
                .build();

        EventoEntrada.ZonaConfigurada zonaConfig = EventoEntrada.ZonaConfigurada.builder()
                .codigoZona("VIP")
                .precioVenta(BigDecimal.valueOf(150))
                .build();

        eventoEntrada = EventoEntrada.builder()
                .id("ee-123")
                .eventoId("evt-123")
                .sedeId("sede-123")
                .zonas(List.of(zonaConfig))
                .build();

        requestValido = new OrdenCompraRequest(
                "user-123",
                "evt-123",
                "sede-123",
                "VIP",
                "A1",
                "token-reserva-uuid"
        );
    }

    @Test
    @DisplayName("Debe crear una orden de compra exitosamente en estado PENDIENTE con el precio configurado")
    void crearOrdenExito() {
        when(usuarioRepository.findById("user-123")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById("evt-123")).thenReturn(Optional.of(evento));
        when(sedeRepository.findById("sede-123")).thenReturn(Optional.of(sede));
        when(eventoEntradaRepository.findByEventoId("evt-123")).thenReturn(Optional.of(eventoEntrada));
        when(reservaService.validarReservaActiva("evt-123", "sede-123", "VIP", "A1", "token-reserva-uuid"))
                .thenReturn(true);
        when(ordenCompraRepository.existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
                anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(false);

        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> {
            OrdenCompra oc = invocation.getArgument(0);
            oc.setId("orden-123");
            return oc;
        });

        OrdenCompraResponse response = ordenCompraService.crear(requestValido);

        assertNotNull(response);
        assertEquals("orden-123", response.id());
        assertEquals("user-123", response.usuarioId());
        assertEquals("evt-123", response.eventoId());
        assertEquals("VIP", response.codigoZona());
        assertEquals("A1", response.codigoAsiento());
        assertEquals(BigDecimal.valueOf(150), response.importe());
        assertEquals(OrdenCompra.EstadoOrden.PENDIENTE, response.estado());
        assertEquals("token-reserva-uuid", response.tokenReserva());
        assertNotNull(response.fechaCreacion());
    }

    @Test
    @DisplayName("Debe lanzar CONFLICT si la reserva en Redis es inválida o expiró")
    void crearOrdenFallaReservaInvalida() {
        when(usuarioRepository.findById("user-123")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById("evt-123")).thenReturn(Optional.of(evento));
        when(sedeRepository.findById("sede-123")).thenReturn(Optional.of(sede));
        when(eventoEntradaRepository.findByEventoId("evt-123")).thenReturn(Optional.of(eventoEntrada));
        when(reservaService.validarReservaActiva("evt-123", "sede-123", "VIP", "A1", "token-reserva-uuid"))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ordenCompraService.crear(requestValido)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("La reserva temporal no existe"));
    }

    @Test
    @DisplayName("Debe lanzar CONFLICT si el asiento ya fue vendido previamente")
    void crearOrdenFallaAsientoVendido() {
        when(usuarioRepository.findById("user-123")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById("evt-123")).thenReturn(Optional.of(evento));
        when(sedeRepository.findById("sede-123")).thenReturn(Optional.of(sede));
        when(eventoEntradaRepository.findByEventoId("evt-123")).thenReturn(Optional.of(eventoEntrada));
        when(reservaService.validarReservaActiva("evt-123", "sede-123", "VIP", "A1", "token-reserva-uuid"))
                .thenReturn(true);
        when(ordenCompraRepository.existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
                eq("evt-123"), eq("sede-123"), eq("VIP"), eq("A1"), anyList()))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ordenCompraService.crear(requestValido)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El asiento ya ha sido vendido"));
    }

    @Test
    @DisplayName("Debe consultar una orden por ID")
    void buscarPorIdExito() {
        OrdenCompra orden = OrdenCompra.builder()
                .id("orden-123")
                .usuarioId("user-123")
                .eventoId("evt-123")
                .sedeId("sede-123")
                .codigoZona("VIP")
                .codigoAsiento("A1")
                .tokenReserva("token-123")
                .importe(BigDecimal.valueOf(150))
                .estado(OrdenCompra.EstadoOrden.PENDIENTE)
                .fechaCreacion(Instant.now())
                .build();

        when(ordenCompraRepository.findById("orden-123")).thenReturn(Optional.of(orden));

        OrdenCompraResponse response = ordenCompraService.buscarPorId("orden-123");

        assertNotNull(response);
        assertEquals("orden-123", response.id());
        assertEquals("user-123", response.usuarioId());
    }
}
