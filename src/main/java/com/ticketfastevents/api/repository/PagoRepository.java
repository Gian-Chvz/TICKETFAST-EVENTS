package com.ticketfastevents.api.repository;

import com.ticketfastevents.api.model.Pago;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends MongoRepository<Pago, String> {

    Optional<Pago> findByOrdenCompraId(String ordenCompraId);

    Optional<Pago> findByReferenciaTransaccion(String referenciaTransaccion);

    List<Pago> findByEstado(Pago.EstadoPago estado);
}
