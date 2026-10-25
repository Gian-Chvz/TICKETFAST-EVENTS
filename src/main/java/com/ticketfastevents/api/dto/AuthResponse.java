package com.ticketfastevents.api.dto;

public record AuthResponse(
        String token,
        String tipo,
        String usuarioId,
        String correo,
        String nombres,
        String apellidos,
        String rol,
        long expiraEnMs
) {
}
