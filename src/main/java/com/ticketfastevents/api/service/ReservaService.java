package com.ticketfastevents.api.service;

import com.ticketfastevents.api.model.Entrada;
import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.model.EventoEntrada;
import com.ticketfastevents.api.model.Reserva;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.repository.EntradaRepository;
import com.ticketfastevents.api.repository.EventoEntradaRepository;
import com.ticketfastevents.api.repository.EventoRepository;
import com.ticketfastevents.api.repository.SedeRepository;
import com.ticketfastevents.api.repository.UsuarioRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaService {

    private static final Duration DURACION_RESERVA =
            Duration.ofMinutes(10);

    private static final String PREFIJO_CLAVE =
            "ticketfast:reserva:";

    private final UsuarioRepository usuarioRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventoRepository eventoRepository;
    private final SedeRepository sedeRepository;
    private final EventoEntradaRepository eventoEntradaRepository;
    private final EntradaRepository entradaRepository;

    public ReservaService(
            StringRedisTemplate redisTemplate,
            EventoRepository eventoRepository,
            SedeRepository sedeRepository,
            EventoEntradaRepository eventoEntradaRepository,
            UsuarioRepository usuarioRepository,
            EntradaRepository entradaRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.eventoRepository = eventoRepository;
        this.sedeRepository = sedeRepository;
        this.eventoEntradaRepository = eventoEntradaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entradaRepository = entradaRepository;
    }

    public Reserva reservar(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            String usuarioId
    ) {
        validarCampo(eventoId, "El ID del evento es obligatorio");
        validarCampo(sedeId, "El ID de la sede es obligatorio");
        validarCampo(codigoZona, "El código de zona es obligatorio");
        validarCampo(codigoAsiento, "El código del asiento es obligatorio");
        validarCampo(usuarioId, "El ID del usuario es obligatorio");

        // 1. Verificar que el evento exista.
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El evento no existe"
                ));

        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe un usuario con el ID indicado"
            );
        }

        // 2. Solo se permiten reservas para eventos activos.
        if (evento.getEstado() != Evento.EstadoEvento.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento no está activo"
            );
        }

        // 3. Verificar que la sede exista.
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La sede no existe"
                ));

        // 4. Buscar la configuración de zonas y precios del evento.
        EventoEntrada configuracion = eventoEntradaRepository
                .findByEventoId(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El evento todavía no tiene zonas configuradas"
                ));

        if (!sedeId.equals(configuracion.getSedeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La sede no corresponde a la configuración del evento"
            );
        }

        // 5. Verificar que la zona esté configurada para el evento.
        boolean zonaConfigurada = configuracion.getZonas()
                .stream()
                .anyMatch(z -> codigoZona.equals(z.getCodigoZona()));

        if (!zonaConfigurada) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La zona no está configurada para este evento"
            );
        }


        // 6. Verificar que el asiento pertenezca a esa zona de la sede.
        boolean asientoExiste = sede.getZonas()
                .stream()
                .filter(z -> codigoZona.equals(z.getCodigo()))
                .flatMap(z -> z.getAsientos().stream())
                .anyMatch(a -> codigoAsiento.equals(a.getCodigo()));

        if (!asientoExiste) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El asiento no existe en la zona indicada"
            );
        }

        // 7. Verificar que el asiento no haya sido vendido de forma definitiva.
        boolean yaVendido = entradaRepository
                .existsByEventoIdAndSedeIdAndCodigoZonaAndCodigoAsientoAndEstadoIn(
                        eventoId,
                        sedeId,
                        codigoZona,
                        codigoAsiento,
                        List.of(Entrada.EstadoEntrada.EMITIDA, Entrada.EstadoEntrada.UTILIZADA)
                );

        if (yaVendido) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento ya ha sido vendido de forma definitiva"
            );
        }

        // 8. Intentar bloquear el asiento atómicamente en Redis.
        String clave = construirClave(
                eventoId,
                sedeId,
                codigoZona,
                codigoAsiento
        );

        String token = UUID.randomUUID().toString();
        Instant ahora = Instant.now();

        Boolean reservada = redisTemplate.opsForValue()
                .setIfAbsent(clave, token, DURACION_RESERVA);

        if (!Boolean.TRUE.equals(reservada)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento ya tiene una reserva temporal activa"
            );
        }

        return new Reserva(
                eventoId,
                sedeId,
                codigoZona,
                codigoAsiento,
                usuarioId,
                token,
                ahora,
                ahora.plus(DURACION_RESERVA)
        );
    }

    public void liberarReserva(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            String tokenReserva
    ) {
        validarCampo(eventoId, "El ID del evento es obligatorio");
        validarCampo(sedeId, "El ID de la sede es obligatorio");
        validarCampo(codigoZona, "El código de zona es obligatorio");
        validarCampo(codigoAsiento, "El código del asiento es obligatorio");
        validarCampo(tokenReserva, "El token de reserva es obligatorio");

        String clave = construirClave(
                eventoId,
                sedeId,
                codigoZona,
                codigoAsiento
        );

        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        DefaultRedisScript<Long> redisScript =
                new DefaultRedisScript<>();

        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long resultado = redisTemplate.execute(
                redisScript,
                Collections.singletonList(clave),
                tokenReserva
        );

        if (resultado == null || resultado == 0L) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva no existe, expiró o el token no coincide"
            );
        }
    }

    public boolean validarReservaActiva(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento,
            String tokenReserva
    ) {
        if (eventoId == null || sedeId == null || codigoZona == null
                || codigoAsiento == null || tokenReserva == null) {
            return false;
        }

        String clave = construirClave(
                eventoId,
                sedeId,
                codigoZona,
                codigoAsiento
        );

        String tokenActual = redisTemplate.opsForValue().get(clave);
        return tokenActual != null && tokenActual.equals(tokenReserva);
    }

    private String construirClave(
            String eventoId,
            String sedeId,
            String codigoZona,
            String codigoAsiento
    ) {
        return PREFIJO_CLAVE
                + codificar(eventoId) + ":"
                + codificar(sedeId) + ":"
                + codificar(codigoZona) + ":"
                + codificar(codigoAsiento);
    }

    private String codificar(String valor) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        valor.getBytes(StandardCharsets.UTF_8)
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
