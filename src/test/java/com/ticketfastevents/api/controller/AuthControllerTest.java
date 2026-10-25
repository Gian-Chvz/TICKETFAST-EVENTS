package com.ticketfastevents.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketfastevents.api.dto.AuthResponse;
import com.ticketfastevents.api.dto.LoginRequest;
import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.security.UserPrincipal;
import com.ticketfastevents.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 200 OK con el token de acceso")
    void loginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest("ana@example.com", "password123");

        AuthResponse response = new AuthResponse(
                "token-jwt-mock",
                "Bearer",
                "usr-1",
                "ana@example.com",
                "Ana",
                "Torres",
                "ROLE_CLIENTE",
                86400000L
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-mock"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuarioId").value("usr-1"))
                .andExpect(jsonPath("$.correo").value("ana@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 201 Created con token emitido")
    void registroEndpoint() throws Exception {
        UsuarioRequest request = new UsuarioRequest(
                "Ana",
                "Torres",
                "ana@example.com",
                "password123",
                "+51987654321",
                "ROLE_CLIENTE"
        );

        AuthResponse response = new AuthResponse(
                "token-jwt-register",
                "Bearer",
                "usr-1",
                "ana@example.com",
                "Ana",
                "Torres",
                "ROLE_CLIENTE",
                86400000L
        );

        when(authService.registrar(any(UsuarioRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-jwt-register"))
                .andExpect(jsonPath("$.usuarioId").value("usr-1"));
    }

    @Test
    @DisplayName("POST /api/auth/logout debe retornar 200 OK con mensaje de cierre de sesión")
    void logoutEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada exitosamente"));
    }

    @Test
    @DisplayName("GET /api/auth/me debe retornar 200 OK con el perfil del usuario autenticado")
    void meEndpoint() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                "usr-1",
                "ana@example.com",
                "secret",
                "Ana",
                "Torres",
                "ROLE_CLIENTE",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

        UsuarioResponse response = new UsuarioResponse(
                "usr-1",
                "Ana",
                "Torres",
                "ana@example.com",
                "+51987654321",
                "ROLE_CLIENTE",
                Instant.now(),
                true
        );

        when(authService.obtenerPerfil(any(UserPrincipal.class))).thenReturn(response);

        try {
            mockMvc.perform(get("/api/auth/me")
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("usr-1"))
                    .andExpect(jsonPath("$.correo").value("ana@example.com"))
                    .andExpect(jsonPath("$.rol").value("ROLE_CLIENTE"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
