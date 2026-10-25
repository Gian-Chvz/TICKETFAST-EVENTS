package com.ticketfastevents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evento_entradas")
public class EventoEntrada {

    @Id
    private String id;

    private String eventoId;

    private String sedeId;

    @Builder.Default
    private List<ZonaConfigurada> zonas = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZonaConfigurada {

        private String codigoZona;

        private BigDecimal precioVenta;
    }
}
