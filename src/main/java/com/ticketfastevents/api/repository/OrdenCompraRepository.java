package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.OrdenCompra;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends MongoRepository<OrdenCompra, String> {

    List<OrdenCompra> findByUsuarioId(String usuarioId);

    List<OrdenCompra> findByEventoId(String eventoId);

    Optional<OrdenCompra> findByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstado(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            OrdenCompra.EstadoOrden estado
    );

    boolean existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            List<OrdenCompra.EstadoOrden> estados
    );
}
