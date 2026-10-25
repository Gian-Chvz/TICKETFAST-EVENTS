package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EntradaResponse;
import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Pago;
import com.ticketfastevents.api.repository.EntradaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EntradaService {

    private final EntradaRepository entradaRepository;

    public EntradaService(EntradaRepository entradaRepository) {
        this.entradaRepository = entradaRepository;
    }

    public EntradaResponse emitirEntrada(OrdenCompra orden, Pago pago) {
        if (orden == null || pago == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La orden de compra y el pago son obligatorios para emitir la entrada"
            );
        }

        // Generar código de ticket único con prefijo reconocible
        String codigoTicket = "TICKET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Entrada entrada = Entrada.builder()
                .codigoTicket(codigoTicket)
                .ordenCompraId(orden.getId())
                .pagoId(pago.getId())
                .usuarioId(orden.getUsuarioId())
                .eventoId(orden.getEventoId())
                .sedeId(orden.getSedeId())
                .codigoZona(orden.getCodigoZona())
                .codigoAsiento(orden.getCodigoAsiento())
                .precio(orden.getImporte())
                .estado(Entrada.EstadoEntrada.EMITIDA)
                .fechaEmision(Instant.now())
                .build();

        Entrada guardada = entradaRepository.save(entrada);
        return convertirAResponse(guardada);
    }

    public List<EntradaResponse> listarTodas() {
        return entradaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public EntradaResponse buscarPorId(String id) {
        validarCampo(id, "El ID de la entrada es obligatorio");

        Entrada entrada = entradaRepository.findById(id.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una entrada con el ID indicado"
                ));

        return convertirAResponse(entrada);
    }

    public EntradaResponse buscarPorCodigoTicket(String codigoTicket) {
        validarCampo(codigoTicket, "El código del ticket es obligatorio");

        Entrada entrada = entradaRepository.findByCodigoTicket(codigoTicket.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una entrada con el código de ticket indicado"
                ));

        return convertirAResponse(entrada);
    }

    public EntradaResponse buscarPorOrdenCompraId(String ordenCompraId) {
        validarCampo(ordenCompraId, "El ID de la orden de compra es obligatorio");

        Entrada entrada = entradaRepository.findByOrdenCompraId(ordenCompraId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró entrada asociada a la orden indicada"
                ));

        return convertirAResponse(entrada);
    }

    public List<EntradaResponse> listarPorUsuarioId(String usuarioId) {
        validarCampo(usuarioId, "El ID del usuario es obligatorio");

        return entradaRepository.findByUsuarioId(usuarioId.trim())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<EntradaResponse> listarPorEventoId(String eventoId) {
        validarCampo(eventoId, "El ID del evento es obligatorio");

        return entradaRepository.findByEventoId(eventoId.trim())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public EntradaResponse validarUsoTicket(String codigoTicket) {
        validarCampo(codigoTicket, "El código del ticket es obligatorio");

        Entrada entrada = entradaRepository.findByCodigoTicket(codigoTicket.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una entrada con el código de ticket indicado"
                ));

        if (entrada.getEstado() == Entrada.EstadoEntrada.UTILIZADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La entrada ya ha sido utilizada previamente"
            );
        }

        if (entrada.getEstado() == Entrada.EstadoEntrada.ANULADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La entrada se encuentra anulada"
            );
        }

        entrada.setEstado(Entrada.EstadoEntrada.UTILIZADA);
        Entrada actualizada = entradaRepository.save(entrada);

        return convertirAResponse(actualizada);
    }

    public EntradaResponse convertirAResponse(Entrada entrada) {
        if (entrada == null) {
            return null;
        }
        return new EntradaResponse(
                entrada.getId(),
                entrada.getCodigoTicket(),
                entrada.getOrdenCompraId(),
                entrada.getPagoId(),
                entrada.getUsuarioId(),
                entrada.getEventoId(),
                entrada.getSedeId(),
                entrada.getCodigoZona(),
                entrada.getCodigoAsiento(),
                entrada.getPrecio(),
                entrada.getEstado(),
                entrada.getFechaEmision()
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
