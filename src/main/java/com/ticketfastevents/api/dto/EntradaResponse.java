package com.ticketfastevents.api.dto;

import com.ticketfastevents.api.model.Entrada;

import java.math.BigDecimal;
import java.time.Instant;

public record EntradaResponse(
        String id,
        String codigoTicket,
        String ordenCompraId,
        String pagoId,
        String usuarioId,
        String eventoId,
        String sedeId,
        String codigoZona,
        String codigoAsiento,
        BigDecimal precio,
        Entrada.EstadoEntrada estado,
        Instant fechaEmision
) {
}
