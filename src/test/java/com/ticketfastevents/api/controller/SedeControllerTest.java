package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketfastevents.api.dto.SedeRequest;
import com.ticketfastevents.api.dto.SedeResponse;
import com.ticketfastevents.api.service.SedeService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SedeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SedeService sedeService;

    @InjectMocks
    private SedeController sedeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sedeController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/sedes debe crear una sede y retornar 201 Created")
    void crearSede() throws Exception {
        SedeRequest.AsientoRequest asiento = new SedeRequest.AsientoRequest("VIP-A1", "A", 1);
        SedeRequest.ZonaRequest zona = new SedeRequest.ZonaRequest(
                "VIP",
                "Zona VIP",
                new BigDecimal("150.00"),
                List.of(asiento)
        );

        SedeRequest request = new SedeRequest(
                "Arena 1",
                "Costa Verde s/n",
                "Lima",
                List.of(zona)
        );

        SedeResponse.AsientoResponse asientoResp = new SedeResponse.AsientoResponse("VIP-A1", "A", 1);
        SedeResponse.ZonaResponse zonaResp = new SedeResponse.ZonaResponse(
                "VIP",
                "Zona VIP",
                new BigDecimal("150.00"),
                List.of(asientoResp)
        );

        SedeResponse response = new SedeResponse(
                "sede-1",
                "Arena 1",
                "Costa Verde s/n",
                "Lima",
                List.of(zonaResp)
        );

        when(sedeService.crear(any(SedeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sede-1"))
                .andExpect(jsonPath("$.nombre").value("Arena 1"))
                .andExpect(jsonPath("$.zonas.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/sedes debe listar todas las sedes")
    void listarSedes() throws Exception {
        SedeResponse response = new SedeResponse(
                "sede-1",
                "Arena 1",
                "Costa Verde s/n",
                "Lima",
                List.of()
        );

        when(sedeService.listarTodas()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/sedes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Arena 1"));
    }

    @Test
    @DisplayName("GET /api/sedes/{id} debe obtener una sede por ID")
    void buscarPorId() throws Exception {
        SedeResponse response = new SedeResponse(
                "sede-1",
                "Arena 1",
                "Costa Verde s/n",
                "Lima",
                List.of()
        );

        when(sedeService.buscarPorId("sede-1")).thenReturn(response);

        mockMvc.perform(get("/api/sedes/sede-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sede-1"))
                .andExpect(jsonPath("$.nombre").value("Arena 1"));
    }

    @Test
    @DisplayName("DELETE /api/sedes/{id} debe eliminar la sede")
    void eliminarSede() throws Exception {
        doNothing().when(sedeService).eliminar("sede-1");

        mockMvc.perform(delete("/api/sedes/sede-1"))
                .andExpect(status().isNoContent());

        verify(sedeService, times(1)).eliminar("sede-1");
    }
}
