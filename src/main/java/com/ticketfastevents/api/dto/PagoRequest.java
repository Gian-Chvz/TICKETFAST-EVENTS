package com.ticketfastevents.api.dto;

import com.ticketfastevents.api.model.Pago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PagoRequest(

        @NotBlank(message = "El ID de la orden de compra es obligatorio")
        String ordenCompraId,

        @NotNull(message = "El método de pago es obligatorio")
        Pago.MetodoPago metodoPago,

        Boolean simularAprobado,

        String referenciaTransaccion
) {
}
