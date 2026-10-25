package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.OrdenCompraRequest;
import com.ticketfastevents.api.dto.OrdenCompraResponse;
import com.ticketfastevents.api.service.OrdenCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "6. Órdenes de Compra (MongoDB)", description = "Creación y consulta de órdenes de compra (Requiere Autenticación)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @Operation(summary = "Crear orden de compra a partir de una reserva activa")
    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crear(
            @Valid @RequestBody OrdenCompraRequest request
    ) {
        OrdenCompraResponse orden = ordenCompraService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orden);
    }

    @Operation(summary = "Listar todas las órdenes de compra")
    @GetMapping
    public ResponseEntity<List<OrdenCompraResponse>> listarTodas() {
        return ResponseEntity.ok(ordenCompraService.listarTodas());
    }

    @Operation(summary = "Buscar orden de compra por ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> buscarPorId(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(ordenCompraService.buscarPorId(id));
    }

    @Operation(summary = "Listar órdenes de compra por ID de usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<OrdenCompraResponse>> listarPorUsuario(
            @PathVariable String usuarioId
    ) {
        return ResponseEntity.ok(ordenCompraService.listarPorUsuarioId(usuarioId));
    }

    @Operation(summary = "Listar órdenes de compra por ID de evento")
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<OrdenCompraResponse>> listarPorEvento(
            @PathVariable String eventoId
    ) {
        return ResponseEntity.ok(ordenCompraService.listarPorEventoId(eventoId));
    }
}
