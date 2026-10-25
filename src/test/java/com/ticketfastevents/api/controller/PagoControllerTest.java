package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.dto.PagoRequest;
import com.ticketfastevents.api.dto.PagoResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.Pago;
import com.ticketfastevents.api.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PagoController pagoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/pagos debe procesar y retornar 201 Created para pago aprobado")
    void procesarPagoAprobadoEndpoint() throws Exception {
        PagoRequest request = new PagoRequest(
                "ord-1",
                Pago.MetodoPago.TARJETA_CREDITO,
                true,
                "REF-123"
        );

        EntradaResponse entrada = new EntradaResponse(
                "ent-1",
                "TICKET-ABC",
                "ord-1",
                "pago-1",
                "user-1",
                "evt-1",
                "sede-1",
                "VIP",
                "A1",
                BigDecimal.valueOf(150),
                Entrada.EstadoEntrada.EMITIDA,
                Instant.now()
        );

        PagoResponse response = new PagoResponse(
                "pago-1",
                "ord-1",
                "REF-123",
                Pago.MetodoPago.TARJETA_CREDITO,
                BigDecimal.valueOf(150),
                Pago.EstadoPago.APROBADO,
                null,
                Instant.now(),
                entrada
        );

        when(pagoService.procesarPago(any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pago-1"))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.entrada.codigoTicket").value("TICKET-ABC"));
    }

    @Test
    @DisplayName("POST /api/pagos debe retornar 200 OK para pago rechazado")
    void procesarPagoRechazadoEndpoint() throws Exception {
        PagoRequest request = new PagoRequest(
                "ord-1",
                Pago.MetodoPago.TARJETA_CREDITO,
                false,
                "REF-123"
        );

        PagoResponse response = new PagoResponse(
                "pago-1",
                "ord-1",
                "REF-123",
                Pago.MetodoPago.TARJETA_CREDITO,
                BigDecimal.valueOf(150),
                Pago.EstadoPago.RECHAZADO,
                "Fondos insuficientes",
                Instant.now(),
                null
        );

        when(pagoService.procesarPago(any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pago-1"))
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("Fondos insuficientes"));
    }

    @Test
    @DisplayName("GET /api/pagos/{id} debe retornar 200 OK con el pago")
    void buscarPorIdEndpoint() throws Exception {
        PagoResponse response = new PagoResponse(
                "pago-1",
                "ord-1",
                "REF-123",
                Pago.MetodoPago.TARJETA_CREDITO,
                BigDecimal.valueOf(150),
                Pago.EstadoPago.APROBADO,
                null,
                Instant.now(),
                null
        );

        when(pagoService.buscarPorId("pago-1")).thenReturn(response);

        mockMvc.perform(get("/api/pagos/pago-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pago-1"))
                .andExpect(jsonPath("$.referenciaTransaccion").value("REF-123"));
    }

    @Test
    @DisplayName("GET /api/pagos/orden/{ordenCompraId} debe retornar el pago asociado a la orden")
    void buscarPorOrdenCompraEndpoint() throws Exception {
        PagoResponse response = new PagoResponse(
                "pago-1",
                "ord-1",
                "REF-123",
                Pago.MetodoPago.TARJETA_CREDITO,
                BigDecimal.valueOf(150),
                Pago.EstadoPago.APROBADO,
                null,
                Instant.now(),
                null
        );

        when(pagoService.buscarPorOrdenCompraId("ord-1")).thenReturn(response);

        mockMvc.perform(get("/api/pagos/orden/ord-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenCompraId").value("ord-1"));
    }
}
