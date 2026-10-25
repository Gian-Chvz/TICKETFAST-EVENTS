package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketfastevents.api.dto.OrdenCompraRequest;
import com.ticketfastevents.api.dto.OrdenCompraResponse;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.service.OrdenCompraService;
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
class OrdenCompraControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private OrdenCompraService ordenCompraService;

    @InjectMocks
    private OrdenCompraController ordenCompraController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ordenCompraController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/ordenes debe retornar 201 Created y los datos de la orden")
    void crearOrdenEndpoint() throws Exception {
        OrdenCompraRequest request = new OrdenCompraRequest(
                "user-1",
                "evt-1",
                "sede-1",
                "ZONA1",
                "A1",
                "token-uuid"
        );

        OrdenCompraResponse response = new OrdenCompraResponse(
                "ord-1",
                "user-1",
                "evt-1",
                "sede-1",
                "ZONA1",
                "A1",
                "token-uuid",
                BigDecimal.valueOf(120),
                OrdenCompra.EstadoOrden.PENDIENTE,
                Instant.now(),
                Instant.now()
        );

        when(ordenCompraService.crear(any(OrdenCompraRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ord-1"))
                .andExpect(jsonPath("$.usuarioId").value("user-1"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.importe").value(120));
    }

    @Test
    @DisplayName("GET /api/ordenes/{id} debe retornar 200 OK")
    void buscarPorIdEndpoint() throws Exception {
        OrdenCompraResponse response = new OrdenCompraResponse(
                "ord-1",
                "user-1",
                "evt-1",
                "sede-1",
                "ZONA1",
                "A1",
                "token-uuid",
                BigDecimal.valueOf(120),
                OrdenCompra.EstadoOrden.PENDIENTE,
                Instant.now(),
                Instant.now()
        );

        when(ordenCompraService.buscarPorId("ord-1")).thenReturn(response);

        mockMvc.perform(get("/api/ordenes/ord-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ord-1"))
                .andExpect(jsonPath("$.usuarioId").value("user-1"));
    }

    @Test
    @DisplayName("GET /api/ordenes/usuario/{usuarioId} debe retornar lista de órdenes")
    void listarPorUsuarioEndpoint() throws Exception {
        OrdenCompraResponse response = new OrdenCompraResponse(
                "ord-1",
                "user-1",
                "evt-1",
                "sede-1",
                "ZONA1",
                "A1",
                "token-uuid",
                BigDecimal.valueOf(120),
                OrdenCompra.EstadoOrden.PENDIENTE,
                Instant.now(),
                Instant.now()
        );

        when(ordenCompraService.listarPorUsuarioId("user-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ordenes/usuario/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ord-1"));
    }
}
