package com.ticketfastevents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "eventos")
public class Evento {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private String tipo;

    private String lugar;

    private LocalDateTime fecha;

    private EstadoEvento estado;

    public enum EstadoEvento {
        ACTIVO,
        CANCELADO,
        FINALIZADO
    }
}
