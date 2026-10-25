package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.dto.PagoRequest;
import com.ticketfastevents.api.dto.PagoResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Pago;
import com.ticketfastevents.api.repository.EntradaRepository;
import com.ticketfastevents.api.repository.OrdenCompraRepository;
import com.ticketfastevents.api.repository.PagoRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private ReservaService reservaService;

    @Mock
    private EntradaService entradaService;

    @Mock
    private EntradaRepository entradaRepository;

    @InjectMocks
    private PagoService pagoService;

    private OrdenCompra ordenPendiente;
    private EntradaResponse entradaResponse;

    @BeforeEach
    void setUp() {
        ordenPendiente = OrdenCompra.builder()
                .id("orden-1")
                .usuarioId("user-1")
                .eventoId("evt-1")
                .sedeId("sede-1")
                .codigoZona("VIP")
                .codigoAsiento("A1")
                .tokenReserva("token-uuid-123")
                .importe(BigDecimal.valueOf(200.00))
                .estado(OrdenCompra.EstadoOrden.PENDIENTE)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        entradaResponse = new EntradaResponse(
                "entrada-1",
                "TICKET-XYZ12345",
                "orden-1",
                "pago-1",
                "user-1",
                "evt-1",
                "sede-1",
                "VIP",
                "A1",
                BigDecimal.valueOf(200.00),
                Entrada.EstadoEntrada.EMITIDA,
                Instant.now()
        );
    }

    @Test
    @DisplayName("Debe procesar y aprobar el pago exitosamente emitiendo la entrada y liberando la reserva en Redis")
    void procesarPago_AprobadoExitoso() {
        PagoRequest request = new PagoRequest(
                "orden-1",
                Pago.MetodoPago.TARJETA_CREDITO,
                true,
                "PAY-REF-001"
        );

        when(pagoRepository.findByReferenciaTransaccion("PAY-REF-001")).thenReturn(Optional.empty());
        when(ordenCompraRepository.findById("orden-1")).thenReturn(Optional.of(ordenPendiente));
        when(reservaService.validarReservaActiva("evt-1", "sede-1", "VIP", "A1", "token-uuid-123")).thenReturn(true);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago p = invocation.getArgument(0);
            p.setId("pago-1");
            return p;
        });
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaService.emitirEntrada(any(OrdenCompra.class), any(Pago.class))).thenReturn(entradaResponse);

        PagoResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertEquals(Pago.EstadoPago.APROBADO, response.estado());
        assertEquals(BigDecimal.valueOf(200.00), response.monto());
        assertEquals("PAY-REF-001", response.referenciaTransaccion());
        assertNotNull(response.entrada());
        assertEquals("TICKET-XYZ12345", response.entrada().codigoTicket());
        assertEquals(OrdenCompra.EstadoOrden.APROBADA, ordenPendiente.getEstado());

        verify(reservaService, times(1)).liberarReserva("evt-1", "sede-1", "VIP", "A1", "token-uuid-123");
        verify(entradaService, times(1)).emitirEntrada(any(OrdenCompra.class), any(Pago.class));
    }

    @Test
    @DisplayName("Debe registrar pago rechazado cuando simularAprobado es false")
    void procesarPago_Rechazado() {
        PagoRequest request = new PagoRequest(
                "orden-1",
                Pago.MetodoPago.TARJETA_DEBITO,
                false,
                null
        );

        when(ordenCompraRepository.findById("orden-1")).thenReturn(Optional.of(ordenPendiente));
        when(reservaService.validarReservaActiva("evt-1", "sede-1", "VIP", "A1", "token-uuid-123")).thenReturn(true);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago p = invocation.getArgument(0);
            p.setId("pago-rechazado-1");
            return p;
        });
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertEquals(Pago.EstadoPago.RECHAZADO, response.estado());
        assertNull(response.entrada());
        assertNotNull(response.motivoRechazo());
        assertEquals(OrdenCompra.EstadoOrden.RECHAZADA, ordenPendiente.getEstado());

        verify(entradaService, never()).emitirEntrada(any(), any());
    }

    @Test
    @DisplayName("Debe ser idempotente si ya existe un pago con la misma referencia")
    void procesarPago_Idempotencia_PorReferencia() {
        PagoRequest request = new PagoRequest(
                "orden-1",
                Pago.MetodoPago.TARJETA_CREDITO,
                true,
                "PAY-REF-EXISTENTE"
        );

        Pago pagoExistente = Pago.builder()
                .id("pago-existente-id")
                .ordenCompraId("orden-1")
                .referenciaTransaccion("PAY-REF-EXISTENTE")
                .metodoPago(Pago.MetodoPago.TARJETA_CREDITO)
                .monto(BigDecimal.valueOf(200.00))
                .estado(Pago.EstadoPago.APROBADO)
                .fechaPago(Instant.now())
                .build();

        when(pagoRepository.findByReferenciaTransaccion("PAY-REF-EXISTENTE")).thenReturn(Optional.of(pagoExistente));
        when(entradaRepository.findByOrdenCompraId("orden-1")).thenReturn(Optional.of(Entrada.builder().id("ent-1").build()));
        when(entradaService.convertirAResponse(any())).thenReturn(entradaResponse);

        PagoResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertEquals("pago-existente-id", response.id());
        assertEquals("PAY-REF-EXISTENTE", response.referenciaTransaccion());
        verify(ordenCompraRepository, never()).findById(anyString());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar el pago si la reserva temporal en Redis ha expirado")
    void procesarPago_ReservaExpirada() {
        PagoRequest request = new PagoRequest(
                "orden-1",
                Pago.MetodoPago.TARJETA_CREDITO,
                true,
                null
        );

        when(ordenCompraRepository.findById("orden-1")).thenReturn(Optional.of(ordenPendiente));
        when(reservaService.validarReservaActiva(any(), any(), any(), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pagoService.procesarPago(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ha expirado"));
        assertEquals(OrdenCompra.EstadoOrden.CANCELADA, ordenPendiente.getEstado());
        verify(ordenCompraRepository, times(1)).save(ordenPendiente);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si la orden de compra no existe")
    void procesarPago_OrdenNoExiste() {
        PagoRequest request = new PagoRequest(
                "orden-inexistente",
                Pago.MetodoPago.TARJETA_CREDITO,
                true,
                null
        );

        when(ordenCompraRepository.findById("orden-inexistente")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pagoService.procesarPago(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
