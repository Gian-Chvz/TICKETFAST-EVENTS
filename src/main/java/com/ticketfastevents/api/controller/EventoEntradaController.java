package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.EventoEntradaRequest;
import com.ticketfastevents.api.dto.EventoEntradaResponse;
import com.ticketfastevents.api.service.EventoEntradaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4. Configuración de Entradas", description = "Consulta pública de precios de entradas y configuración por evento/sede")
@RestController
@RequestMapping("/api/evento-entradas")
public class EventoEntradaController {

    private final EventoEntradaService eventoEntradaService;

    public EventoEntradaController(
            EventoEntradaService eventoEntradaService) {
        this.eventoEntradaService = eventoEntradaService;
    }

    @Operation(summary = "Configurar entradas y precios para evento (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventoEntradaResponse> crear(
            @Valid @RequestBody EventoEntradaRequest request) {

        EventoEntradaResponse respuesta =
                eventoEntradaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(summary = "Listar todas las configuraciones de entradas (Público)")
    @GetMapping
    public ResponseEntity<List<EventoEntradaResponse>> listarTodas() {
        return ResponseEntity.ok(
                eventoEntradaService.listarTodas()
        );
    }

    @Operation(summary = "Buscar configuración de entradas por ID de evento (Público)")
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<EventoEntradaResponse> buscarPorEvento(
            @PathVariable String eventoId) {

        return ResponseEntity.ok(
                eventoEntradaService.buscarPorEventoId(eventoId)
        );
    }
}
