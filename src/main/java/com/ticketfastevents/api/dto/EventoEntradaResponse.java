package com.ticketfastevents.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventoEntradaResponse(
        String id,
        String eventoId,
        String sedeId,
        List<ZonaEntradaResponse> zonas
) {

    public record ZonaEntradaResponse(
            String codigoZona,
            BigDecimal precioVenta
    ) {
    }
}
