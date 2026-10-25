package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.SedeRequest;
import com.ticketfastevents.api.dto.SedeResponse;
import com.ticketfastevents.api.service.SedeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(
            @Valid @RequestBody SedeRequest request) {

        SedeResponse respuesta = sedeService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<SedeResponse>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeResponse> buscarPorId(
            @PathVariable String id) {

        return ResponseEntity.ok(sedeService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String id) {

        sedeService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
