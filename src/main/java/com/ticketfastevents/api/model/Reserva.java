package com.ticketfastevents.api.model;

import java.time.Instant;

public record Reserva(
        String eventoId,
        String sedeId,
        String codigoZona,
        String codigoAsiento,
        String usuarioId,
        String tokenReserva,
        Instant fechaCreacion,
        Instant fechaExpiracion
) {
}
