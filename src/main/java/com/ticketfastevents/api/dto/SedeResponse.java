
package com.ticketfastevents.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SedeResponse(
        String id,
        String nombre,
        String direccion,
        String ciudad,
        List<ZonaResponse> zonas
) {

    public record ZonaResponse(
            String codigo,
            String nombre,
            BigDecimal precioBase,
            List<AsientoResponse> asientos
    ) {
    }

    public record AsientoResponse(
            String codigo,
            String fila,
            Integer numero
    ) {
    }
}
