package com.ticketfastevents.api.dto;

import java.time.Instant;

public record UsuarioResponse(
        String id,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        Instant fechaRegistro,
        boolean activo
) {}
