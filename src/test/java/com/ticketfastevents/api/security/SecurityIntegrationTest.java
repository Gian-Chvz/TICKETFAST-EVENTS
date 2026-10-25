package com.ticketfastevents.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketfastevents.api.controller.AuthController;
import com.ticketfastevents.api.controller.ReservaController;
import com.ticketfastevents.api.dto.LoginRequest;
import com.ticketfastevents.api.dto.ReservaRequest;
import com.ticketfastevents.api.model.Reserva;
import com.ticketfastevents.api.service.AuthService;
import com.ticketfastevents.api.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityIntegrationTest {

    private MockMvc mockMvcReservas;
    private MockMvc mockMvcAuth;
    private JwtService jwtService;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Mock
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService("TicketFastSuperSecretKeyForJwtAuthenticationSecurityWithAtLeast256Bits2026", 86400000L);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);

        ReservaController reservaController = new ReservaController(reservaService);
        AuthController authController = new AuthController(authService);

        mockMvcReservas = MockMvcBuilders.standaloneSetup(reservaController)
                .addFilters(jwtFilter)
                .build();

        mockMvcAuth = MockMvcBuilders.standaloneSetup(authController)
                .addFilters(jwtFilter)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Petición con token JWT válido establece la autenticación en el SecurityContext")
    void peticionConTokenValido() throws Exception {
        String token = jwtService.generarToken(
                "usr-1",
                "ana@example.com",
                "ROLE_CLIENTE",
                "Ana",
                "Torres"
        );

        ReservaRequest reservaRequest = new ReservaRequest(
                "evt-1",
                "sede-1",
                "VIP",
                "A1",
                "usr-1"
        );

        Reserva reservaMock = new Reserva(
                "evt-1",
                "sede-1",
                "VIP",
                "A1",
                "usr-1",
                "token-uuid",
                Instant.now(),
                Instant.now().plusSeconds(600)
        );

        when(reservaService.reservar(any(), any(), any(), any(), any())).thenReturn(reservaMock);

        mockMvcReservas.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value("usr-1"));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ana@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Petición a login procesa correctamente")
    void peticionLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("ana@example.com", "password123");

        mockMvcAuth.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }
}
