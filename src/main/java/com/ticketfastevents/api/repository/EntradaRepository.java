package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.Entrada;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntradaRepository extends MongoRepository<Entrada, String> {

    Optional<Entrada> findByCodigoTicket(String codigoTicket);

    Optional<Entrada> findByOrdenCompraId(String ordenCompraId);

    List<Entrada> findByUsuarioId(String usuarioId);

    List<Entrada> findByEventoId(String eventoId);

    boolean existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            List<Entrada.EstadoEntrada> estados
    );
}
