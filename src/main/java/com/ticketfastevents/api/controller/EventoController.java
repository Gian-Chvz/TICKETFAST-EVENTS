package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.EventoRequest;
import com.ticketfastevents.api.dto.EventoResponse;
import com.ticketfastevents.api.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(
            @Valid @RequestBody EventoRequest request) {

        EventoResponse respuesta = eventoService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listarTodos() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(
            @PathVariable String id) {

        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody EventoRequest request) {

        return ResponseEntity.ok(
                eventoService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String id) {

        eventoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
