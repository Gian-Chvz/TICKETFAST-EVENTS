package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.EventoEntrada;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EventoEntradaRepository
        extends MongoRepository<EventoEntrada, String> {

    Optional<EventoEntrada> findByEventoId(String eventoId);

    boolean existsByEventoId(String eventoId);
}
