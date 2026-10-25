package com.ticketfastevents.api.config;

import com.ticketfastevents.api.model.Evento;
import com.ticketfastevents.api.model.EventoEntrada;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.EventoEntradaRepository;
import com.ticketfastevents.api.repository.EventoRepository;
import com.ticketfastevents.api.repository.SedeRepository;
import com.ticketfastevents.api.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final EventoRepository eventoRepository;
    private final EventoEntradaRepository eventoEntradaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UsuarioRepository usuarioRepository,
            SedeRepository sedeRepository,
            EventoRepository eventoRepository,
            EventoEntradaRepository eventoEntradaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sedeRepository = sedeRepository;
        this.eventoRepository = eventoRepository;
        this.eventoEntradaRepository = eventoEntradaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            inicializarUsuarios();
            inicializarSedeYEvento();
        } catch (Exception e) {
            log.warn("No se pudo completar la inicialización de datos de prueba: {}", e.getMessage());
        }
    }

    private void inicializarUsuarios() {
        if (usuarioRepository.findByCorreoIgnoreCase("admin@ticketfast.com").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .nombres("Administrador")
                    .apellidos("TicketFast")
                    .correo("admin@ticketfast.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .telefono("+51999999999")
                    .rol("ROLE_ADMIN")
                    .activo(true)
                    .build();
            usuarioRepository.save(admin);
            log.info("Usuario Administrador precargado: admin@ticketfast.com / Admin123! (ROLE_ADMIN)");
        }

        if (usuarioRepository.findByCorreoIgnoreCase("cliente@ticketfast.com").isEmpty()) {
            Usuario cliente = Usuario.builder()
                    .nombres("Cliente")
                    .apellidos("Demo")
                    .correo("cliente@ticketfast.com")
                    .password(passwordEncoder.encode("Cliente123!"))
                    .telefono("+51988888888")
                    .rol("ROLE_CLIENTE")
                    .activo(true)
                    .build();
            usuarioRepository.save(cliente);
            log.info("Usuario Cliente precargado: cliente@ticketfast.com / Cliente123! (ROLE_CLIENTE)");
        }
    }

    private void inicializarSedeYEvento() {
        if (sedeRepository.count() == 0) {
            Sede.Asiento asientoVip1 = Sede.Asiento.builder().codigo("VIP-A1").fila("A").numero(1).build();
            Sede.Asiento asientoVip2 = Sede.Asiento.builder().codigo("VIP-A2").fila("A").numero(2).build();
            Sede.Asiento asientoVip3 = Sede.Asiento.builder().codigo("VIP-A3").fila("A").numero(3).build();

            Sede.Asiento asientoGen1 = Sede.Asiento.builder().codigo("GEN-B1").fila("B").numero(1).build();
            Sede.Asiento asientoGen2 = Sede.Asiento.builder().codigo("GEN-B2").fila("B").numero(2).build();

            Sede.Zona zonaVip = Sede.Zona.builder()
                    .codigo("VIP")
                    .nombre("Zona VIP")
                    .precioBase(new BigDecimal("150.00"))
                    .asientos(List.of(asientoVip1, asientoVip2, asientoVip3))
                    .build();

            Sede.Zona zonaGen = Sede.Zona.builder()
                    .codigo("GENERAL")
                    .nombre("Zona General")
                    .precioBase(new BigDecimal("80.00"))
                    .asientos(List.of(asientoGen1, asientoGen2))
                    .build();

            Sede sede = Sede.builder()
                    .nombre("Estadio Nacional")
                    .direccion("Av. Jose Diaz s/n")
                    .ciudad("Lima")
                    .zonas(List.of(zonaVip, zonaGen))
                    .build();

            Sede sedeGuardada = sedeRepository.save(sede);
            log.info("Sede de prueba precargada: {} (ID: {})", sedeGuardada.getNombre(), sedeGuardada.getId());

            if (eventoRepository.count() == 0) {
                Evento evento = Evento.builder()
                        .nombre("Concierto de Rock 2026")
                        .descripcion("Gran gira internacional de rock en vivo.")
                        .tipo("CONCIERTO")
                        .lugar("Estadio Nacional")
                        .fecha(LocalDateTime.now().plusMonths(2))
                        .estado(Evento.EstadoEvento.ACTIVO)
                        .build();

                Evento eventoGuardado = eventoRepository.save(evento);
                log.info("Evento de prueba precargado: {} (ID: {})", eventoGuardado.getNombre(), eventoGuardado.getId());

                EventoEntrada.ZonaConfigurada zonaConfVip = EventoEntrada.ZonaConfigurada.builder()
                        .codigoZona("VIP")
                        .precioVenta(new BigDecimal("200.00"))
                        .build();

                EventoEntrada.ZonaConfigurada zonaConfGen = EventoEntrada.ZonaConfigurada.builder()
                        .codigoZona("GENERAL")
                        .precioVenta(new BigDecimal("100.00"))
                        .build();

                EventoEntrada eventoEntrada = EventoEntrada.builder()
                        .eventoId(eventoGuardado.getId())
                        .sedeId(sedeGuardada.getId())
                        .zonas(List.of(zonaConfVip, zonaConfGen))
                        .build();

                eventoEntradaRepository.save(eventoEntrada);
                log.info("Configuración de precios de entradas precargada para evento ID: {}", eventoGuardado.getId());
            }
        }
    }
}
