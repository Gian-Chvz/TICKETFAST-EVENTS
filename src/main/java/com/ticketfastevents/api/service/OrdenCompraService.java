package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.OrdenCompraRequest;
import com.ticketfastevents.api.dto.OrdenCompraResponse;
import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.model.EventoEntrada;
import com.ticketfastevents.api.model.OrdenCompra;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.EventoEntradaRepository;
import com.ticketfastevents.api.repository.EventoRepository;
import com.ticketfastevents.api.repository.OrdenCompraRepository;
import com.ticketfastevents.api.repository.SedeRepository;
import com.ticketfastevents.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final SedeRepository sedeRepository;
    private final EventoEntradaRepository eventoEntradaRepository;
    private final ReservaService reservaService;

    public OrdenCompraService(
            OrdenCompraRepository ordenCompraRepository,
            UsuarioRepository usuarioRepository,
            EventoRepository eventoRepository,
            SedeRepository sedeRepository,
            EventoEntradaRepository eventoEntradaRepository,
            ReservaService reservaService
    ) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.sedeRepository = sedeRepository;
        this.eventoEntradaRepository = eventoEntradaRepository;
        this.reservaService = reservaService;
    }

    public OrdenCompraResponse crear(OrdenCompraRequest request) {
        validarCampo(request.usuarioId(), "El ID del usuario es obligatorio");
        validarCampo(request.eventoId(), "El ID del evento es obligatorio");
        validarCampo(request.sedeId(), "El ID de la sede es obligatorio");
        validarCampo(request.codigoZona(), "El código de zona es obligatorio");
        validarCampo(request.codigoAsiento(), "El código del asiento es obligatorio");
        validarCampo(request.tokenReserva(), "El token de reserva es obligatorio");

        // 1. Validar que el usuario exista y esté activo
        Usuario usuario = usuarioRepository.findById(request.usuarioId().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un usuario con el ID indicado"
                ));

        if (!usuario.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario no se encuentra activo"
            );
        }

        // 2. Validar que el evento exista y esté activo
        Evento evento = eventoRepository.findById(request.eventoId().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El evento no existe"
                ));

        if (evento.getEstado() != Evento.EstadoEvento.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento no está activo"
            );
        }

        // 3. Validar que la sede exista
        Sede sede = sedeRepository.findById(request.sedeId().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La sede no existe"
                ));

        // 4. Validar configuración de entradas y obtener precio de venta
        EventoEntrada configuracion = eventoEntradaRepository
                .findByEventoId(request.eventoId().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El evento todavía no tiene zonas configuradas"
                ));

        if (!request.sedeId().trim().equals(configuracion.getSedeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La sede no corresponde a la configuración del evento"
            );
        }

        BigDecimal precioVenta = configuracion.getZonas()
                .stream()
                .filter(z -> request.codigoZona().trim().equals(z.getCodigoZona()))
                .map(EventoEntrada.ZonaConfigurada::getPrecioVenta)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La zona no está configurada para este evento"
                ));

        // 5. Validar que el asiento exista físicamente en la zona de la sede
        boolean asientoExiste = sede.getZonas()
                .stream()
                .filter(z -> request.codigoZona().trim().equals(z.getCodigo()))
                .flatMap(z -> z.getAsientos().stream())
                .anyMatch(a -> request.codigoAsiento().trim().equals(a.getCodigo()));

        if (!asientoExiste) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El asiento no existe en la zona indicada"
            );
        }

        // 6. Validar reserva temporal activa en Redis con el token suministrado
        boolean reservaValida = reservaService.validarReservaActiva(
                request.eventoId().trim(),
                request.sedeId().trim(),
                request.codigoZona().trim(),
                request.codigoAsiento().trim(),
                request.tokenReserva().trim()
        );

        if (!reservaValida) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva temporal no existe, expiró o el token no coincide"
            );
        }

        // 7. Validar que no exista una orden ya aprobada para este asiento
        boolean ordenExistente = ordenCompraRepository
                .existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
                        request.eventoId().trim(),
                        request.sedeId().trim(),
                        request.codigoZona().trim(),
                        request.codigoAsiento().trim(),
                        List.of(OrdenCompra.EstadoOrden.APROBADA)
                );

        if (ordenExistente) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento ya ha sido vendido"
            );
        }

        // 8. Crear y guardar la orden de compra
        Instant ahora = Instant.now();
        OrdenCompra orden = OrdenCompra.builder()
                .usuarioId(request.usuarioId().trim())
                .eventoId(request.eventoId().trim())
                .sedeId(request.sedeId().trim())
                .codigoZona(request.codigoZona().trim())
                .codigoAsiento(request.codigoAsiento().trim())
                .tokenReserva(request.tokenReserva().trim())
                .importe(precioVenta)
                .estado(OrdenCompra.EstadoOrden.PENDIENTE)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();

        OrdenCompra guardada = ordenCompraRepository.save(orden);

        return convertirAResponse(guardada);
    }

    public List<OrdenCompraResponse> listarTodas() {
        return ordenCompraRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public OrdenCompraResponse buscarPorId(String id) {
        validarCampo(id, "El ID de la orden es obligatorio");

        OrdenCompra orden = ordenCompraRepository.findById(id.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una orden de compra con el ID indicado"
                ));

        return convertirAResponse(orden);
    }

    public List<OrdenCompraResponse> listarPorUsuarioId(String usuarioId) {
        validarCampo(usuarioId, "El ID del usuario es obligatorio");

        return ordenCompraRepository.findByUsuarioId(usuarioId.trim())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<OrdenCompraResponse> listarPorEventoId(String eventoId) {
        validarCampo(eventoId, "El ID del evento es obligatorio");

        return ordenCompraRepository.findByEventoId(eventoId.trim())
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private OrdenCompraResponse convertirAResponse(OrdenCompra orden) {
        return new OrdenCompraResponse(
                orden.getId(),
                orden.getUsuarioId(),
                orden.getEventoId(),
                orden.getSedeId(),
                orden.getCodigoZona(),
                orden.getCodigoAsiento(),
                orden.getTokenReserva(),
                orden.getImporte(),
                orden.getEstado(),
                orden.getFechaCreacion(),
                orden.getFechaActualizacion()
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
