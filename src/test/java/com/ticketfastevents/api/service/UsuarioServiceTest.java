package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Debe registrar usuario con contraseña encriptada y rol asignado")
    void registrarUsuarioExitoso() {
        UsuarioRequest request = new UsuarioRequest(
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "password123",
                "+51999888777",
                "ROLE_CLIENTE"
        );

        when(usuarioRepository.existsByCorreoIgnoreCase("carlos@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId("usr-1");
            return u;
        });

        UsuarioResponse response = usuarioService.registrar(request);

        assertNotNull(response);
        assertEquals("usr-1", response.id());
        assertEquals("Carlos", response.nombres());
        assertEquals("carlos@example.com", response.correo());
        assertEquals("ROLE_CLIENTE", response.rol());
    }

    @Test
    @DisplayName("Debe lanzar excepción 409 Conflict si el correo ya existe")
    void registrarCorreoDuplicado() {
        UsuarioRequest request = new UsuarioRequest(
                "Carlos",
                "Gomez",
                "carlos@example.com",
                "password123",
                null,
                null
        );

        when(usuarioRepository.existsByCorreoIgnoreCase("carlos@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> usuarioService.registrar(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void obtenerPorIdExitoso() {
        Usuario usuario = Usuario.builder()
                .id("usr-1")
                .nombres("Carlos")
                .apellidos("Gomez")
                .correo("carlos@example.com")
                .rol("ROLE_CLIENTE")
                .activo(true)
                .build();

        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.obtenerPorId("usr-1");

        assertNotNull(response);
        assertEquals("usr-1", response.id());
        assertEquals("carlos@example.com", response.correo());
    }
}
