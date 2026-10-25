package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.service.EntradaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EntradaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EntradaService entradaService;

    @InjectMocks
    private EntradaController entradaController;

    private EntradaResponse entradaResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(entradaController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        entradaResponse = new EntradaResponse(
                "ent-1",
                "TICKET-ABC123XYZ",
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
    }

    @Test
    @DisplayName("GET /api/entradas/{id} debe retornar 200 OK con la entrada")
    void buscarPorIdEndpoint() throws Exception {
        when(entradaService.buscarPorId("ent-1")).thenReturn(entradaResponse);

        mockMvc.perform(get("/api/entradas/ent-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ent-1"))
                .andExpect(jsonPath("$.codigoTicket").value("TICKET-ABC123XYZ"));
    }

    @Test
    @DisplayName("GET /api/entradas/ticket/{codigoTicket} debe retornar 200 OK")
    void buscarPorCodigoTicketEndpoint() throws Exception {
        when(entradaService.buscarPorCodigoTicket("TICKET-ABC123XYZ")).thenReturn(entradaResponse);

        mockMvc.perform(get("/api/entradas/ticket/TICKET-ABC123XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoTicket").value("TICKET-ABC123XYZ"));
    }

    @Test
    @DisplayName("GET /api/entradas/usuario/{usuarioId} debe retornar lista de entradas")
    void listarPorUsuarioEndpoint() throws Exception {
        when(entradaService.listarPorUsuarioId("user-1")).thenReturn(List.of(entradaResponse));

        mockMvc.perform(get("/api/entradas/usuario/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value("user-1"));
    }

    @Test
    @DisplayName("PATCH /api/entradas/ticket/{codigoTicket}/validar-uso debe marcar ticket como utilizado")
    void validarUsoTicketEndpoint() throws Exception {
        EntradaResponse entradaUtilizada = new EntradaResponse(
                "ent-1",
                "TICKET-ABC123XYZ",
                "ord-1",
                "pago-1",
                "user-1",
                "evt-1",
                "sede-1",
                "VIP",
                "A1",
                BigDecimal.valueOf(150),
                Entrada.EstadoEntrada.UTILIZADA,
                Instant.now()
        );

        when(entradaService.validarUsoTicket("TICKET-ABC123XYZ")).thenReturn(entradaUtilizada);

        mockMvc.perform(patch("/api/entradas/ticket/TICKET-ABC123XYZ/validar-uso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UTILIZADA"));
    }
}
