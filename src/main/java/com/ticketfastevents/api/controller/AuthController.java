package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.AuthResponse;
import com.ticketfastevents.api.dto.LoginRequest;
import com.ticketfastevents.api.dto.MensajeResponse;
import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.security.UserPrincipal;
import com.ticketfastevents.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Autenticación y Registro", description = "Endpoints públicos de acceso libre para registro de nuevos clientes, inicio y cierre de sesión")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión (Público)", description = "Autentica con correo y contraseña retornando el token JWT.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registro libre de nuevo cliente (Público)", description = "Crea una nueva cuenta y retorna inmediatamente el token JWT listo para operar.")
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        AuthResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cerrar sesión (Público)", description = "Confirma el cierre de sesión para que el cliente descarte el token JWT.")
    @PostMapping("/logout")
    public ResponseEntity<MensajeResponse> logout() {
        return ResponseEntity.ok(new MensajeResponse("Sesión cerrada exitosamente"));
    }

    @Operation(summary = "Consultar perfil del usuario autenticado", description = "Retorna los datos del usuario en sesión.", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        UsuarioResponse response = authService.obtenerPerfil(principal);
        return ResponseEntity.ok(response);
    }
}
