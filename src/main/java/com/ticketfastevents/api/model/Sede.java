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
@Document(collection = "sedes")
public class Sede {

    @Id
    private String id;

    private String nombre;

    private String direccion;

    private String ciudad;

    @Builder.Default
    private List<Zona> zonas = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Zona {

        private String codigo;

        private String nombre;

        private BigDecimal precioBase;

        @Builder.Default
        private List<Asiento> asientos = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Asiento {

        private String codigo;

        private String fila;

        private Integer numero;
    }
}

