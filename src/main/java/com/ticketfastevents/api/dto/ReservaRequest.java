package com.ticketfastevents.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservaRequest(

        @NotBlank(message = "El ID del evento es obligatorio")
        String eventoId,

        @NotBlank(message = "El ID de la sede es obligatorio")
        String sedeId,

        @NotBlank(message = "El código de zona es obligatorio")
        @Size(max = 50)
        String codigoZona,

        @NotBlank(message = "El código del asiento es obligatorio")
        @Size(max = 50)
        String codigoAsiento,

        @NotBlank(message = "El ID del usuario es obligatorio")
        String usuarioId

) {
}
