package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.Evento;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventoRepository extends MongoRepository<Evento, String> {
}