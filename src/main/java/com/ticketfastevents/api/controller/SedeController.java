package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.SedeRequest;
import com.ticketfastevents.api.dto.SedeResponse;
import com.ticketfastevents.api.service.SedeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. Sedes y Distribución", description = "Consulta pública de sedes, zonas y asientos, y administración de recintos")
@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    @Operation(summary = "Crear nueva sede con zonas y asientos (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SedeResponse> crear(
            @Valid @RequestBody SedeRequest request) {

        SedeResponse respuesta = sedeService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @Operation(summary = "Listar todas las sedes (Público)")
    @GetMapping
    public ResponseEntity<List<SedeResponse>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }

    @Operation(summary = "Buscar sede por ID (Público)")
    @GetMapping("/{id}")
    public ResponseEntity<SedeResponse> buscarPorId(
            @PathVariable String id) {

        return ResponseEntity.ok(sedeService.buscarPorId(id));
    }

    @Operation(summary = "Eliminar sede por ID (Requiere ROLE_ADMIN)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String id) {

        sedeService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
