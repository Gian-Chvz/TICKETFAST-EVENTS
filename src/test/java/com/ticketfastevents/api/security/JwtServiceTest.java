package com.ticketfastevents.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "TicketFastSuperSecretKeyForJwtAuthenticationSecurityWithAtLeast256Bits2026";
    private static final long EXPIRATION_MS = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Debe generar un token válido y extraer correctamente sus claims")
    void generarYValidarToken() {
        String token = jwtService.generarToken(
                "usr-123",
                "ana.torres@example.com",
                "ROLE_CLIENTE",
                "Ana",
                "Torres"
        );

        assertNotNull(token);
        assertTrue(jwtService.esTokenValido(token));
        assertEquals("ana.torres@example.com", jwtService.extraerCorreo(token));
        assertEquals("usr-123", jwtService.extraerUsuarioId(token));
        assertEquals("ROLE_CLIENTE", jwtService.extraerRol(token));
    }

    @Test
    @DisplayName("Debe invalidar tokens con firma alterada o expirados")
    void tokenInvalido() {
        String token = jwtService.generarToken(
                "usr-123",
                "ana.torres@example.com",
                "ROLE_CLIENTE",
                "Ana",
                "Torres"
        );

        String tokenManipulado = token + "modificado";
        assertFalse(jwtService.esTokenValido(tokenManipulado));
    }

    @Test
    @DisplayName("Debe detectar tokens expirados inmediatamente")
    void tokenExpirado() {
        JwtService jwtServiceExpirado = new JwtService(SECRET, -1000); // Expirado en el pasado
        String tokenExpirado = jwtServiceExpirado.generarToken(
                "usr-123",
                "ana.torres@example.com",
                "ROLE_CLIENTE",
                "Ana",
                "Torres"
        );

        assertFalse(jwtService.esTokenValido(tokenExpirado));
    }
}
