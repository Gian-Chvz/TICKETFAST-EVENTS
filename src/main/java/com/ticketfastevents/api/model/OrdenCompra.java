package com.ticketfastevents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ordenes_compra")
public class OrdenCompra {

    @Id
    private String id;

    private String usuarioId;

    private String eventoId;

    private String sedeId;

    private String codigoZona;

    private String codigoAsiento;

    private String tokenReserva;

    private BigDecimal importe;

    @Builder.Default
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    private Instant fechaActualizacion;

    public enum EstadoOrden {
        PENDIENTE,
        APROBADA,
        RECHAZADA,
        CANCELADA
    }
}
