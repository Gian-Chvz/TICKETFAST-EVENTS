package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.PagoRequest;
import com.ticketfastevents.api.dto.PagoResponse;
import com.ticketfastevents.api.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "7. Pagos Simulados (MongoDB)", description = "Procesamiento de pagos y control de idempotencia (Requiere Autenticación)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Procesar pago simulado con control de idempotencia")
    @PostMapping({"", "/procesar"})
    public ResponseEntity<PagoResponse> procesarPago(
            @Valid @RequestBody PagoRequest request
    ) {
        PagoResponse response = pagoService.procesarPago(request);

        HttpStatus status = (response.estado() == com.ticketfastevents.api.model.Pago.EstadoPago.APROBADO)
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @Operation(summary = "Listar todos los pagos")
    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarTodos() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    @Operation(summary = "Buscar pago por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> buscarPorId(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(pagoService.buscarPorId(id));
    }

    @Operation(summary = "Buscar pago por ID de orden de compra")
    @GetMapping("/orden/{ordenCompraId}")
    public ResponseEntity<PagoResponse> buscarPorOrdenCompra(
            @PathVariable String ordenCompraId
    ) {
        return ResponseEntity.ok(pagoService.buscarPorOrdenCompraId(ordenCompraId));
    }

    @Operation(summary = "Buscar pago por referencia de transacción")
    @GetMapping("/referencia/{referenciaTransaccion}")
    public ResponseEntity<PagoResponse> buscarPorReferencia(
            @PathVariable String referenciaTransaccion
    ) {
        return ResponseEntity.ok(pagoService.buscarPorReferencia(referenciaTransaccion));
    }
}
