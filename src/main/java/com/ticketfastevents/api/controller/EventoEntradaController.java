package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.EventoEntradaRequest;
import com.ticketfastevents.api.dto.EventoEntradaResponse;
import com.ticketfastevents.api.service.EventoEntradaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evento-entradas")
public class EventoEntradaController {

    private final EventoEntradaService eventoEntradaService;

    public EventoEntradaController(
            EventoEntradaService eventoEntradaService) {
        this.eventoEntradaService = eventoEntradaService;
    }

    @PostMapping
    public ResponseEntity<EventoEntradaResponse> crear(
            @Valid @RequestBody EventoEntradaRequest request) {

        EventoEntradaResponse respuesta =
                eventoEntradaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EventoEntradaResponse>> listarTodas() {
        return ResponseEntity.ok(
                eventoEntradaService.listarTodas()
        );
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<EventoEntradaResponse> buscarPorEvento(
            @PathVariable String eventoId) {

        return ResponseEntity.ok(
                eventoEntradaService.buscarPorEventoId(eventoId)
        );
    }
}
