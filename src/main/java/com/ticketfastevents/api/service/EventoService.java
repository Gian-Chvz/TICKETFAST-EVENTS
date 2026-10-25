package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.EventoRequest;
import com.ticketfastevents.api.dto.EventoResponse;
import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.repository.EventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public EventoResponse crear(EventoRequest request) {
        Evento evento = Evento.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tipo(request.tipo())
                .lugar(request.lugar())
                .fecha(request.fecha())
                .estado(Evento.EstadoEvento.valueOf(request.estado().name()))
                .build();

        Evento eventoGuardado = eventoRepository.save(evento);

        return convertirAResponse(eventoGuardado);
    }

    public List<EventoResponse> listarTodos() {
        return eventoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public EventoResponse buscarPorId(String id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el evento con ID: " + id
                ));

        return convertirAResponse(evento);
    }

    public EventoResponse actualizar(String id, EventoRequest request) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el evento con ID: " + id
                ));

        evento.setNombre(request.nombre());
        evento.setDescripcion(request.descripcion());
        evento.setTipo(request.tipo());
        evento.setLugar(request.lugar());
        evento.setFecha(request.fecha());
        evento.setEstado(
                Evento.EstadoEvento.valueOf(request.estado().name())
        );

        Evento eventoActualizado = eventoRepository.save(evento);

        return convertirAResponse(eventoActualizado);
    }

    public void eliminar(String id) {
        if (!eventoRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el evento con ID: " + id
            );
        }

        eventoRepository.deleteById(id);
    }

    private EventoResponse convertirAResponse(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getTipo(),
                evento.getLugar(),
                evento.getFecha(),
                evento.getEstado().name()
        );
    }
}
