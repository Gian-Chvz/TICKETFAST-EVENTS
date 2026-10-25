package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "8. Administración de Usuarios", description = "Operaciones administrativas de gestión y consulta de usuarios (Exclusivo ROLE_ADMIN)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Crear usuario desde administración (Solo ADMIN)")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @Valid @RequestBody UsuarioRequest request) {

        UsuarioResponse usuario = usuarioService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    @Operation(summary = "Listar todos los usuarios (Solo ADMIN)")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @Operation(summary = "Obtener usuario por ID (Solo ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(
            @PathVariable String id) {

        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }
}
