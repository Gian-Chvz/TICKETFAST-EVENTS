package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.Sede;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SedeRepository extends MongoRepository<Sede, String> {

    boolean existsByNombreIgnoreCase(String nombre);
}
