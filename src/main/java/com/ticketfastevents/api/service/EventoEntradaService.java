package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EventoEntradaRequest;
import com.ticketfastevents.api.dto.EventoEntradaResponse;
import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.model.EventoEntrada;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.repository.EventoEntradaRepository;
import com.ticketfastevents.api.repository.EventoRepository;
import com.ticketfastevents.api.repository.SedeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EventoEntradaService {

    private final EventoEntradaRepository eventoEntradaRepository;
    private final EventoRepository eventoRepository;
    private final SedeRepository sedeRepository;

    public EventoEntradaService(
            EventoEntradaRepository eventoEntradaRepository,
            EventoRepository eventoRepository,
            SedeRepository sedeRepository) {

        this.eventoEntradaRepository = eventoEntradaRepository;
        this.eventoRepository = eventoRepository;
        this.sedeRepository = sedeRepository;
    }

    public EventoEntradaResponse crear(EventoEntradaRequest request) {

        // Verificar que el evento exista.
        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el evento indicado"
                ));

        // Verificar que el evento pueda recibir una configuración de venta.
        if (evento.getEstado() != Evento.EstadoEvento.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden configurar entradas para eventos activos"
            );
        }

        // Verificar que la sede exista.
        Sede sede = sedeRepository.findById(request.sedeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la sede indicada"
                ));

        // Evitar dos configuraciones para el mismo evento.
        if (eventoEntradaRepository.existsByEventoId(request.eventoId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento ya tiene una configuración de entradas"
            );
        }

        // Verificar que no se repitan zonas en la solicitud.
        Set<String> codigosSolicitados = new HashSet<>();

        for (EventoEntradaRequest.ZonaEntradaRequest zona
                : request.zonas()) {

            String codigo = zona.codigoZona()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!codigosSolicitados.add(codigo)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La solicitud contiene una zona repetida: "
                                + zona.codigoZona()
                );
            }

            // Verificar que la zona pertenezca a la sede.
            boolean zonaExiste = sede.getZonas()
                    .stream()
                    .anyMatch(z ->
                            z.getCodigo() != null
                                    && z.getCodigo().trim()
                                    .equalsIgnoreCase(codigo)
                    );

            if (!zonaExiste) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La zona " + zona.codigoZona()
                                + " no pertenece a la sede indicada"
                );
            }
        }

        List<EventoEntrada.ZonaConfigurada> zonas =
                request.zonas()
                        .stream()
                        .map(zona -> EventoEntrada.ZonaConfigurada.builder()
                                .codigoZona(zona.codigoZona().trim())
                                .precioVenta(zona.precioVenta())
                                .build())
                        .toList();

        EventoEntrada configuracion = EventoEntrada.builder()
                .eventoId(evento.getId())
                .sedeId(sede.getId())
                .zonas(zonas)
                .build();

        EventoEntrada guardada =
                eventoEntradaRepository.save(configuracion);

        return convertirAResponse(guardada);
    }

    public EventoEntradaResponse buscarPorEventoId(String eventoId) {

        EventoEntrada configuracion =
                eventoEntradaRepository.findByEventoId(eventoId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "El evento no tiene configuración de entradas"
                        ));

        return convertirAResponse(configuracion);
    }

    public List<EventoEntradaResponse> listarTodas() {
        return eventoEntradaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private EventoEntradaResponse convertirAResponse(
            EventoEntrada configuracion) {

        List<EventoEntradaResponse.ZonaEntradaResponse> zonas =
                configuracion.getZonas()
                        .stream()
                        .map(zona ->
                                new EventoEntradaResponse.ZonaEntradaResponse(
                                        zona.getCodigoZona(),
                                        zona.getPrecioVenta()
                                ))
                        .toList();

        return new EventoEntradaResponse(
                configuracion.getId(),
                configuracion.getEventoId(),
                configuracion.getSedeId(),
                zonas
        );
    }
}
