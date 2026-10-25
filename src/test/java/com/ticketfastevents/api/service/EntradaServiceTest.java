package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Pago;
import com.ticketfastevents.api.repository.EntradaRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock
    private EntradaRepository entradaRepository;

    @InjectMocks
    private EntradaService entradaService;

    private OrdenCompra orden;
    private Pago pago;
    private Entrada entrada;

    @BeforeEach
    void setUp() {
        orden = OrdenCompra.builder()
                .id("orden-1")
                .usuarioId("user-1")
                .eventoId("evt-1")
                .sedeId("sede-1")
                .codigoZona("VIP")
                .codigoAsiento("A1")
                .importe(BigDecimal.valueOf(150.00))
                .estado(OrdenCompra.EstadoOrden.APROBADA)
                .build();

        pago = Pago.builder()
                .id("pago-1")
                .ordenCompraId("orden-1")
                .referenciaTransaccion("PAY-12345")
                .metodoPago(Pago.MetodoPago.TARJETA_CREDITO)
                .monto(BigDecimal.valueOf(150.00))
                .estado(Pago.EstadoPago.APROBADO)
                .build();

        entrada = Entrada.builder()
                .id("entrada-1")
                .codigoTicket("TICKET-ABC123XYZ")
                .ordenCompraId("orden-1")
                .pagoId("pago-1")
                .usuarioId("user-1")
                .eventoId("evt-1")
                .sedeId("sede-1")
                .codigoZona("VIP")
                .codigoAsiento("A1")
                .precio(BigDecimal.valueOf(150.00))
                .estado(Entrada.EstadoEntrada.EMITIDA)
                .fechaEmision(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Debe emitir entrada correctamente al recibir orden y pago aprobado")
    void emitirEntrada_Exitoso() {
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> {
            Entrada e = invocation.getArgument(0);
            e.setId("entrada-generada-id");
            return e;
        });

        EntradaResponse response = entradaService.emitirEntrada(orden, pago);

        assertNotNull(response);
        assertEquals("orden-1", response.ordenCompraId());
        assertEquals("pago-1", response.pagoId());
        assertEquals("user-1", response.usuarioId());
        assertEquals("VIP", response.codigoZona());
        assertEquals("A1", response.codigoAsiento());
        assertEquals(Entrada.EstadoEntrada.EMITIDA, response.estado());
        assertTrue(response.codigoTicket().startsWith("TICKET-"));
        verify(entradaRepository, times(1)).save(any(Entrada.class));
    }

    @Test
    @DisplayName("Debe buscar entrada por ID")
    void buscarPorId_Exitoso() {
        when(entradaRepository.findById("entrada-1")).thenReturn(Optional.of(entrada));

        EntradaResponse response = entradaService.buscarPorId("entrada-1");

        assertNotNull(response);
        assertEquals("entrada-1", response.id());
        assertEquals("TICKET-ABC123XYZ", response.codigoTicket());
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si la entrada no existe")
    void buscarPorId_NoExiste() {
        when(entradaRepository.findById("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                entradaService.buscarPorId("no-existe")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe buscar entrada por código de ticket")
    void buscarPorCodigoTicket_Exitoso() {
        when(entradaRepository.findByCodigoTicket("TICKET-ABC123XYZ")).thenReturn(Optional.of(entrada));

        EntradaResponse response = entradaService.buscarPorCodigoTicket("ticket-abc123xyz");

        assertNotNull(response);
        assertEquals("entrada-1", response.id());
    }

    @Test
    @DisplayName("Debe validar y marcar ticket como utilizado")
    void validarUsoTicket_Exitoso() {
        when(entradaRepository.findByCodigoTicket("TICKET-ABC123XYZ")).thenReturn(Optional.of(entrada));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntradaResponse response = entradaService.validarUsoTicket("TICKET-ABC123XYZ");

        assertNotNull(response);
        assertEquals(Entrada.EstadoEntrada.UTILIZADA, response.estado());
    }

    @Test
    @DisplayName("Debe rechazar uso de ticket ya utilizado")
    void validarUsoTicket_YaUtilizado() {
        entrada.setEstado(Entrada.EstadoEntrada.UTILIZADA);
        when(entradaRepository.findByCodigoTicket("TICKET-ABC123XYZ")).thenReturn(Optional.of(entrada));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                entradaService.validarUsoTicket("TICKET-ABC123XYZ")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe listar entradas por usuario y por evento")
    void listarPorUsuarioYEvento() {
        when(entradaRepository.findByUsuarioId("user-1")).thenReturn(List.of(entrada));
        when(entradaRepository.findByEventoId("evt-1")).thenReturn(List.of(entrada));

        List<EntradaResponse> porUsuario = entradaService.listarPorUsuarioId("user-1");
        List<EntradaResponse> porEvento = entradaService.listarPorEventoId("evt-1");

        assertEquals(1, porUsuario.size());
        assertEquals(1, porEvento.size());
    }
}
