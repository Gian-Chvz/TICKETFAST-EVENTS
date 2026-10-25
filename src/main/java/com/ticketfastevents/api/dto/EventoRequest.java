package com.ticketfastevents.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventoRequest(

        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        @NotBlank(message = "El tipo de evento es obligatorio")
        String tipo,

        @NotBlank(message = "El lugar del evento es obligatorio")
        String lugar,

        @NotNull(message = "La fecha del evento es obligatoria")
        @Future(message = "La fecha del evento debe ser futura")
        LocalDateTime fecha,

        @NotNull(message = "El estado del evento es obligatorio")
        EventoEstado estado
) {
    public enum EventoEstado {
        ACTIVO,
        CANCELADO,
        FINALIZADO
    }
}
