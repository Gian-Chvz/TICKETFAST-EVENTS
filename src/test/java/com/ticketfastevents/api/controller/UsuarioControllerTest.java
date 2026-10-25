package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.service.UsuarioService;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/usuarios debe registrar un usuario y retornar 201 Created")
    void registrarUsuario() throws Exception {
        UsuarioRequest request = new UsuarioRequest(
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "password123",
                "+51999888777",
                "ROLE_ADMIN"
        );

        UsuarioResponse response = new UsuarioResponse(
                "usr-1",
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "+51999888777",
                "ROLE_ADMIN",
                Instant.now(),
                true
        );

        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("usr-1"))
                .andExpect(jsonPath("$.correo").value("carlos@example.com"))
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/usuarios debe listar todos los usuarios")
    void listarUsuarios() throws Exception {
        UsuarioResponse user1 = new UsuarioResponse(
                "usr-1",
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "+51999888777",
                "ROLE_ADMIN",
                Instant.now(),
                true
        );

        when(usuarioService.listar()).thenReturn(List.of(user1));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].correo").value("carlos@example.com"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} debe obtener usuario por ID")
    void obtenerPorId() throws Exception {
        UsuarioResponse user1 = new UsuarioResponse(
                "usr-1",
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "+51999888777",
                "ROLE_ADMIN",
                Instant.now(),
                true
        );

        when(usuarioService.obtenerPorId("usr-1")).thenReturn(user1);

        mockMvc.perform(get("/api/usuarios/usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr-1"))
                .andExpect(jsonPath("$.correo").value("carlos@example.com"));
    }
}
