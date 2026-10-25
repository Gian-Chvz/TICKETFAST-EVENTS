package com.ticketfastevents.api.dto;

import java.time.LocalDateTime;

public record EventoResponse(
        String id,
        String nombre,
        String descripcion,
        String tipo,
        String lugar,
        LocalDateTime fecha,
        String estado
) {
}
