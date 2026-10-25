package com.ticketfastevents.api.dto;

import jakarta.validation.constraints.NotBlank;

public record OrdenCompraRequest(

        @NotBlank(message = "El ID del usuario es obligatorio")
        String usuarioId,

        @NotBlank(message = "El ID del evento es obligatorio")
        String eventoId,

        @NotBlank(message = "El ID de la sede es obligatorio")
        String sedeId,

        @NotBlank(message = "El código de zona es obligatorio")
        String codigoZona,

        @NotBlank(message = "El código del asiento es obligatorio")
        String codigoAsiento,

        @NotBlank(message = "El token de reserva es obligatorio")
        String tokenReserva
) {
}
