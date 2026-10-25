package com.ticketfastevents.api.dto;

import com.ticketfastevents.api.model.Pago;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoResponse(
        String id,
        String ordenCompraId,
        String referenciaTransaccion,
        Pago.MetodoPago metodoPago,
        BigDecimal monto,
        Pago.EstadoPago estado,
        String motivoRechazo,
        Instant fechaPago,
        EntradaResponse entrada
) {
}
