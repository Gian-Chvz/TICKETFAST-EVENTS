package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.AuthResponse;
import com.ticketfastevents.api.dto.LoginRequest;
import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.UsuarioRepository;
import com.ticketfastevents.api.security.JwtService;
import com.ticketfastevents.api.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Debe autenticar con éxito y retornar token JWT cuando las credenciales son válidas")
    void loginExitoso() {
        LoginRequest request = new LoginRequest("ana@example.com", "password123");

        Usuario usuario = Usuario.builder()
                .id("usr-1")
                .nombres("Ana")
                .apellidos("Torres")
                .correo("ana@example.com")
                .password("$2a$10$hashedPassword")
                .rol("ROLE_CLIENTE")
                .activo(true)
                .build();

        when(usuarioRepository.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generarToken("usr-1", "ana@example.com", "ROLE_CLIENTE", "Ana", "Torres"))
                .thenReturn("jwt-token-123");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals("usr-1", response.usuarioId());
        assertEquals("ana@example.com", response.correo());
        assertEquals("ROLE_CLIENTE", response.rol());
    }

    @Test
    @DisplayName("Debe rechazar login cuando el usuario no existe")
    void loginUsuarioNoExiste() {
        LoginRequest request = new LoginRequest("noexiste@example.com", "pass123");

        when(usuarioRepository.findByCorreoIgnoreCase("noexiste@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe rechazar login cuando la contraseña es incorrecta")
    void loginPasswordIncorrecto() {
        LoginRequest request = new LoginRequest("ana@example.com", "passIncorrecto");

        Usuario usuario = Usuario.builder()
                .id("usr-1")
                .correo("ana@example.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .build();

        when(usuarioRepository.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("passIncorrecto", "$2a$10$hashedPassword")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe registrar usuario y emitir token automáticamente")
    void registrarExitoso() {
        UsuarioRequest request = new UsuarioRequest(
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "secret123",
                "+51999888777",
                "ROLE_CLIENTE"
        );

        UsuarioResponse usuarioResponse = new UsuarioResponse(
                "usr-2",
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "+51999888777",
                "ROLE_CLIENTE",
                Instant.now(),
                true
        );

        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(usuarioResponse);
        when(jwtService.generarToken(eq("usr-2"), eq("carlos@example.com"), eq("ROLE_CLIENTE"), eq("Carlos"), eq("Gomez")))
                .thenReturn("jwt-token-registered");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.registrar(request);

        assertNotNull(response);
        assertEquals("jwt-token-registered", response.token());
        assertEquals("usr-2", response.usuarioId());
    }
}
