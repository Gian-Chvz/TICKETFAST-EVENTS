package com.ticketfastevents.api.dto;

import java.time.Instant;

public record ReservaResponse(
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
