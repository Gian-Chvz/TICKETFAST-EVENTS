package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.service.EntradaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "7. Emisión y Consulta de Entradas", description = "Consulta y validación de tickets definitivos emitidos tras pago aprobado (Requiere Autenticación)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @Operation(summary = "Listar todas las entradas emitidas")
    @GetMapping
    public ResponseEntity<List<EntradaResponse>> listarTodas() {
        return ResponseEntity.ok(entradaService.listarTodas());
    }

    @Operation(summary = "Buscar entrada por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EntradaResponse> buscarPorId(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(entradaService.buscarPorId(id));
    }

    @Operation(summary = "Buscar entrada por código único de ticket")
    @GetMapping("/ticket/{codigoTicket}")
    public ResponseEntity<EntradaResponse> buscarPorCodigoTicket(
            @PathVariable String codigoTicket
    ) {
        return ResponseEntity.ok(entradaService.buscarPorCodigoTicket(codigoTicket));
    }

    @Operation(summary = "Buscar entrada asociada a una orden de compra")
    @GetMapping("/orden/{ordenCompraId}")
    public ResponseEntity<EntradaResponse> buscarPorOrdenCompra(
            @PathVariable String ordenCompraId
    ) {
        return ResponseEntity.ok(entradaService.buscarPorOrdenCompraId(ordenCompraId));
    }

    @Operation(summary = "Listar entradas por ID de usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<EntradaResponse>> listarPorUsuario(
            @PathVariable String usuarioId
    ) {
        return ResponseEntity.ok(entradaService.listarPorUsuarioId(usuarioId));
    }

    @Operation(summary = "Listar entradas por ID de evento")
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<EntradaResponse>> listarPorEvento(
            @PathVariable String eventoId
    ) {
        return ResponseEntity.ok(entradaService.listarPorEventoId(eventoId));
    }

    @Operation(summary = "Validar el uso de un ticket en puerta de acceso")
    @PatchMapping("/ticket/{codigoTicket}/validar-uso")
    public ResponseEntity<EntradaResponse> validarUsoTicket(
            @PathVariable String codigoTicket
    ) {
        return ResponseEntity.ok(entradaService.validarUsoTicket(codigoTicket));
    }
}
