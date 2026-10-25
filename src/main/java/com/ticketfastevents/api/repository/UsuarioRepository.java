package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends MongoRepository<Usuario, String> {

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
