package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.dto.PagoRequest;
import com.ticketfastevents.api.dto.PagoResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Pago;
import com.ticketfastevents.api.repository.EntradaRepository;
import com.ticketfastevents.api.repository.OrdenCompraRepository;
import com.ticketfastevents.api.repository.PagoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ReservaService reservaService;
    private final EntradaService entradaService;
    private final EntradaRepository entradaRepository;

    public PagoService(
            PagoRepository pagoRepository,
            OrdenCompraRepository ordenCompraRepository,
            ReservaService reservaService,
            EntradaService entradaService,
            EntradaRepository entradaRepository
    ) {
        this.pagoRepository = pagoRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.reservaService = reservaService;
        this.entradaService = entradaService;
        this.entradaRepository = entradaRepository;
    }

    public PagoResponse procesarPago(PagoRequest request) {
        validarCampo(request.ordenCompraId(), "El ID de la orden de compra es obligatorio");
        if (request.metodoPago() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El método de pago es obligatorio"
            );
        }

        // 1. Idempotencia: Verificar si ya existe un pago con la misma referencia de transacción
        if (request.referenciaTransaccion() != null && !request.referenciaTransaccion().isBlank()) {
            Optional<Pago> pagoExistentePorRef = pagoRepository.findByReferenciaTransaccion(request.referenciaTransaccion().trim());
            if (pagoExistentePorRef.isPresent()) {
                return construirPagoResponse(pagoExistentePorRef.get());
            }
        }

        // 2. Validar existencia de la orden de compra
        OrdenCompra orden = ordenCompraRepository.findById(request.ordenCompraId().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una orden de compra con el ID indicado"
                ));

        // 3. Idempotencia a nivel de orden: Si ya fue aprobada, retornar el pago existente
        if (orden.getEstado() == OrdenCompra.EstadoOrden.APROBADA) {
            Optional<Pago> pagoAprobadoExistente = pagoRepository.findByOrdenCompraId(orden.getId());
            if (pagoAprobadoExistente.isPresent()) {
                return construirPagoResponse(pagoAprobadoExistente.get());
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La orden de compra ya ha sido aprobada previamente"
            );
        }

        // 4. Validar estado de la orden
        if (orden.getEstado() == OrdenCompra.EstadoOrden.CANCELADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La orden de compra se encuentra cancelada"
            );
        }

        // 5. Validar que la reserva temporal en Redis siga activa con su token correspondiente
        boolean reservaValida = reservaService.validarReservaActiva(
                orden.getEventoId(),
                orden.getSedeId(),
                orden.getCodigoZona(),
                orden.getCodigoAsiento(),
                orden.getTokenReserva()
        );

        if (!reservaValida) {
            orden.setEstado(OrdenCompra.EstadoOrden.CANCELADA);
            orden.setFechaActualizacion(Instant.now());
            ordenCompraRepository.save(orden);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva temporal en Redis ha expirado. La orden de compra ha sido cancelada."
            );
        }

        // 6. Generar referencia de transacción
        String referencia = (request.referenciaTransaccion() != null && !request.referenciaTransaccion().isBlank())
                ? request.referenciaTransaccion().trim()
                : "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        boolean esAprobado = request.simularAprobado() == null || Boolean.TRUE.equals(request.simularAprobado());
        Instant ahora = Instant.now();

        if (esAprobado) {
            // Registrar pago aprobado
            Pago pago = Pago.builder()
                    .ordenCompraId(orden.getId())
                    .referenciaTransaccion(referencia)
                    .metodoPago(request.metodoPago())
                    .monto(orden.getImporte())
                    .estado(Pago.EstadoPago.APROBADO)
                    .fechaPago(ahora)
                    .build();

            Pago pagoGuardado = pagoRepository.save(pago);

            // Actualizar orden de compra a APROBADA
            orden.setEstado(OrdenCompra.EstadoOrden.APROBADA);
            orden.setFechaActualizacion(ahora);
            ordenCompraRepository.save(orden);

            // Emitir Entrada permanente
            EntradaResponse entradaResponse = entradaService.emitirEntrada(orden, pagoGuardado);

            // Liberar la reserva temporal en Redis puesto que la plaza ya está comprada definitivamente
            try {
                reservaService.liberarReserva(
                        orden.getEventoId(),
                        orden.getSedeId(),
                        orden.getCodigoZona(),
                        orden.getCodigoAsiento(),
                        orden.getTokenReserva()
                );
            } catch (Exception ignored) {
                // Si la clave ya expiró o fue liberada, la persistencia permanente ya está asegurada
            }

            return new PagoResponse(
                    pagoGuardado.getId(),
                    pagoGuardado.getOrdenCompraId(),
                    pagoGuardado.getReferenciaTransaccion(),
                    pagoGuardado.getMetodoPago(),
                    pagoGuardado.getMonto(),
                    pagoGuardado.getEstado(),
                    null,
                    pagoGuardado.getFechaPago(),
                    entradaResponse
            );
        } else {
            // Registrar pago rechazado
            Pago pago = Pago.builder()
                    .ordenCompraId(orden.getId())
                    .referenciaTransaccion(referencia)
                    .metodoPago(request.metodoPago())
                    .monto(orden.getImporte())
                    .estado(Pago.EstadoPago.RECHAZADO)
                    .motivoRechazo("Pago rechazado por el emisor (fondos insuficientes o transacción denegada en simulación)")
                    .fechaPago(ahora)
                    .build();

            Pago pagoGuardado = pagoRepository.save(pago);

            // Actualizar orden de compra a RECHAZADA
            orden.setEstado(OrdenCompra.EstadoOrden.RECHAZADA);
            orden.setFechaActualizacion(ahora);
            ordenCompraRepository.save(orden);

            return new PagoResponse(
                    pagoGuardado.getId(),
                    pagoGuardado.getOrdenCompraId(),
                    pagoGuardado.getReferenciaTransaccion(),
                    pagoGuardado.getMetodoPago(),
                    pagoGuardado.getMonto(),
                    pagoGuardado.getEstado(),
                    pagoGuardado.getMotivoRechazo(),
                    pagoGuardado.getFechaPago(),
                    null
            );
        }
    }

    public List<PagoResponse> listarTodos() {
        return pagoRepository.findAll()
                .stream()
                .map(this::construirPagoResponse)
                .toList();
    }

    public PagoResponse buscarPorId(String id) {
        validarCampo(id, "El ID del pago es obligatorio");

        Pago pago = pagoRepository.findById(id.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un pago con el ID indicado"
                ));

        return construirPagoResponse(pago);
    }

    public PagoResponse buscarPorOrdenCompraId(String ordenCompraId) {
        validarCampo(ordenCompraId, "El ID de la orden de compra es obligatorio");

        Pago pago = pagoRepository.findByOrdenCompraId(ordenCompraId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un pago registrado para la orden de compra indicada"
                ));

        return construirPagoResponse(pago);
    }

    public PagoResponse buscarPorReferencia(String referenciaTransaccion) {
        validarCampo(referenciaTransaccion, "La referencia de transacción es obligatoria");

        Pago pago = pagoRepository.findByReferenciaTransaccion(referenciaTransaccion.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un pago con la referencia de transacción indicada"
                ));

        return construirPagoResponse(pago);
    }

    private PagoResponse construirPagoResponse(Pago pago) {
        EntradaResponse entradaResponse = null;
        if (pago.getEstado() == Pago.EstadoPago.APROBADO) {
            Optional<Entrada> entradaOpt = entradaRepository.findByOrdenCompraId(pago.getOrdenCompraId());
            if (entradaOpt.isPresent()) {
                entradaResponse = entradaService.convertirAResponse(entradaOpt.get());
            }
        }

        return new PagoResponse(
                pago.getId(),
                pago.getOrdenCompraId(),
                pago.getReferenciaTransaccion(),
                pago.getMetodoPago(),
                pago.getMonto(),
                pago.getEstado(),
                pago.getMotivoRechazo(),
                pago.getFechaPago(),
                entradaResponse
        );
    }

    private void validarCampo(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    mensaje
            );
        }
    }
}
