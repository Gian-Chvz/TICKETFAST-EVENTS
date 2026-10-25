package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.EventoRequest;
import com.ticketfastevents.api.dto.EventoResponse;
import com.ticketfastevents.api.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "2. Eventos", description = "Consulta pública del catálogo y administración de eventos")
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Operation(summary = "Crear nuevo evento (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventoResponse> crear(
            @Valid @RequestBody EventoRequest request) {

        EventoResponse respuesta = eventoService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(summary = "Listar todos los eventos (Público)")
    @GetMapping
    public ResponseEntity<List<EventoResponse>> listarTodos() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @Operation(summary = "Buscar evento por ID (Público)")
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(
            @PathVariable String id) {

        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar evento existente (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody EventoRequest request) {

        return ResponseEntity.ok(
                eventoService.actualizar(id, request)
        );
    }

    @Operation(summary = "Eliminar evento por ID (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String id) {

        eventoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
