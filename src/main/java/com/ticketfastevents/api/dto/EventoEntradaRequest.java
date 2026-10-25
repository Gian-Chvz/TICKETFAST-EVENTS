package com.ticketfastevents.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record EventoEntradaRequest(

        @NotBlank(message = "El ID del evento es obligatorio")
        String eventoId,

        @NotBlank(message = "El ID de la sede es obligatorio")
        String sedeId,

        @NotEmpty(message = "Debe habilitarse al menos una zona")
        List<@Valid ZonaEntradaRequest> zonas

) {

    public record ZonaEntradaRequest(

            @NotBlank(message = "El código de la zona es obligatorio")
            String codigoZona,

            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(
                    value = "0.01",
                    message = "El precio de venta debe ser mayor que cero"
            )
            BigDecimal precioVenta

    ) {
    }
}
