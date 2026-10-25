package com.ticketfastevents.api.dto;

import java.time.Instant;

public record UsuarioResponse(
        String id,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String rol,
        Instant fechaRegistro,
        boolean activo
) {}
