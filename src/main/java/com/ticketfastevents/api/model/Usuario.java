package com.ticketfastevents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nombres;

    private String apellidos;

    @Indexed(unique = true)
    private String correo;

    private String telefono;

    @Builder.Default
    private Instant fechaRegistro = Instant.now();

    @Builder.Default
    private boolean activo = true;
}
