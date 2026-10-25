package com.ticketfastevents.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record SedeRequest(

        @NotBlank(message = "El nombre de la sede es obligatorio")
        String nombre,

        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @NotBlank(message = "La ciudad es obligatoria")
        String ciudad,

        @NotEmpty(message = "La sede debe tener al menos una zona")
        List<@Valid ZonaRequest> zonas

) {

    public record ZonaRequest(

            @NotBlank(message = "El código de la zona es obligatorio")
            String codigo,

            @NotBlank(message = "El nombre de la zona es obligatorio")
            String nombre,

            @NotNull(message = "El precio base es obligatorio")
            @DecimalMin(
                    value = "0.01",
                    message = "El precio base debe ser mayor que cero"
            )
            BigDecimal precioBase,

            @NotEmpty(message = "La zona debe tener al menos un asiento")
            List<@Valid AsientoRequest> asientos

    ) {
    }

    public record AsientoRequest(

            @NotBlank(message = "El código del asiento es obligatorio")
            String codigo,

            @NotBlank(message = "La fila del asiento es obligatoria")
            String fila,

            @NotNull(message = "El número del asiento es obligatorio")
            @Positive(message = "El número del asiento debe ser positivo")
            Integer numero

    ) {
    }
}
