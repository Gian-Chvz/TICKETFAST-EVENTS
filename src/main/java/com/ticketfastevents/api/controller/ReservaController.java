package com.ticketfastevents.api.controller;

import com.ticketfastevents.api.dto.ReservaRequest;
import com.ticketfastevents.api.dto.ReservaResponse;
import com.ticketfastevents.api.model.Reserva;
import com.ticketfastevents.api.service.ReservaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. Reservas Temporales (Redis)", description = "Bloqueo atómico temporal de asientos por 10 minutos (Requiere Autenticación)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(summary = "Bloquear temporalmente un asiento en Redis")
    @PostMapping
    public ResponseEntity<ReservaResponse> reservar(
            @Valid @RequestBody ReservaRequest request
    ) {
        Reserva reserva = reservaService.reservar(
                request.eventoId(),
                request.sedeId(),
                request.codigoZona(),
                request.codigoAsiento(),
                request.usuarioId()
        );

        ReservaResponse response = convertirAResponse(reserva);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Liberar bloqueo temporal de asiento mediante token")
    @DeleteMapping
    public ResponseEntity<Void> liberarReserva(
            @RequestParam String eventoId,
            @RequestParam String sedeId,
            @RequestParam String codigoZona,
            @RequestParam String codigoAsiento,
            @RequestParam String tokenReserva
    ) {
        reservaService.liberarReserva(
                eventoId,
                sedeId,
                codigoZona,
                codigoAsiento,
                tokenReserva
        );

        return ResponseEntity.noContent().build();
    }

    private ReservaResponse convertirAResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.eventoId(),
                reserva.sedeId(),
                reserva.codigoZona(),
                reserva.codigoAsiento(),
                reserva.usuarioId(),
                reserva.tokenReserva(),
                reserva.fechaCreacion(),
                reserva.fechaExpiracion()
        );
    }
}
