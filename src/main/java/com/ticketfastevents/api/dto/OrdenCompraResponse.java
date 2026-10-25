package com.ticketfastevents.api.dto;

import com.ticketfastevents.api.model.OrdenCompra;

import java.math.BigDecimal;
import java.time.Instant;

public record OrdenCompraResponse(
        String id,
        String usuarioId,
        String eventoId,
        String sedeId,
        String codigoZona,
        String codigoAsiento,
        String tokenReserva,
        BigDecimal importe,
        OrdenCompra.EstadoOrden estado,
        Instant fechaCreacion,
        Instant fechaActualizacion
) {
}
