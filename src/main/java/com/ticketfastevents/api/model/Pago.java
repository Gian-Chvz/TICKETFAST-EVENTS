package com.ticketfastevents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pagos")
public class Pago {

    @Id
    private String id;

    @Indexed
    private String ordenCompraId;

    @Indexed(unique = true)
    private String referenciaTransaccion;

    private MetodoPago metodoPago;

    private BigDecimal monto;

    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    private String motivoRechazo;

    @Builder.Default
    private Instant fechaPago = Instant.now();

    public enum MetodoPago {
        TARJETA_CREDITO,
        TARJETA_DEBITO,
        TRANSFERENCIA,
        BILLETERA_DIGITAL
    }

    public enum EstadoPago {
        PENDIENTE,
        APROBADO,
        RECHAZADO
    }
}
