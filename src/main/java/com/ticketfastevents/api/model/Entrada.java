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
@Document(collection = "entradas")
public class Entrada {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigoTicket;

    private String ordenCompraId;

    private String pagoId;

    private String usuarioId;

    private String eventoId;

    private String sedeId;

    private String codigoZona;

    private String codigoAsiento;

    private BigDecimal precio;

    @Builder.Default
    private EstadoEntrada estado = EstadoEntrada.EMITIDA;

    @Builder.Default
    private Instant fechaEmision = Instant.now();

    public enum EstadoEntrada {
        EMITIDA,
        UTILIZADA,
        ANULADA
    }
}
