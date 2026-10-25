package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.SedeRequest;
import com.ticketfastevents.api.dto.SedeResponse;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.repository.SedeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SedeService {

    private final SedeRepository sedeRepository;

    public SedeService(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public SedeResponse crear(SedeRequest request) {
        validarNombreDuplicado(request.nombre());
        validarZonasYAsientos(request.zonas());

        Sede sede = convertirAModelo(request);
        Sede sedeGuardada = sedeRepository.save(sede);

        return convertirAResponse(sedeGuardada);
    }

    public List<SedeResponse> listarTodas() {
        return sedeRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public SedeResponse buscarPorId(String id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la sede con ID: " + id
                ));

        return convertirAResponse(sede);
    }

    public void eliminar(String id) {
        if (!sedeRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró la sede con ID: " + id
            );
        }

        sedeRepository.deleteById(id);
    }

    private void validarNombreDuplicado(String nombre) {
        boolean existe = sedeRepository.existsByNombreIgnoreCase(
                nombre.trim()
        );

        if (existe) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una sede con ese nombre"
            );
        }
    }

    private void validarZonasYAsientos(
            List<SedeRequest.ZonaRequest> zonas) {

        Set<String> codigosZona = new HashSet<>();

        for (SedeRequest.ZonaRequest zona : zonas) {

            String codigoZona = zona.codigo()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!codigosZona.add(codigoZona)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Código de zona duplicado: " + zona.codigo()
                );
            }

            Set<String> codigosAsiento = new HashSet<>();
            Set<String> posicionesAsiento = new HashSet<>();

            for (SedeRequest.AsientoRequest asiento : zona.asientos()) {

                String codigoAsiento = asiento.codigo()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                if (!codigosAsiento.add(codigoAsiento)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Código de asiento duplicado en la zona "
                                    + zona.codigo() + ": "
                                    + asiento.codigo()
                    );
                }

                String posicion = asiento.fila()
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        + "-" + asiento.numero();

                if (!posicionesAsiento.add(posicion)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Posición de asiento duplicada en la zona "
                                    + zona.codigo() + ": " + posicion
                    );
                }
            }
        }
    }

    private Sede convertirAModelo(SedeRequest request) {
        List<Sede.Zona> zonas = request.zonas()
                .stream()
                .map(zona -> {
                    List<Sede.Asiento> asientos = zona.asientos()
                            .stream()
                            .map(asiento -> Sede.Asiento.builder()
                                    .codigo(asiento.codigo().trim())
                                    .fila(asiento.fila().trim())
                                    .numero(asiento.numero())
                                    .build())
                            .toList();

                    return Sede.Zona.builder()
                            .codigo(zona.codigo().trim())
                            .nombre(zona.nombre().trim())
                            .precioBase(zona.precioBase())
                            .asientos(asientos)
                            .build();
                })
                .toList();

        return Sede.builder()
                .nombre(request.nombre().trim())
                .direccion(request.direccion().trim())
                .ciudad(request.ciudad().trim())
                .zonas(zonas)
                .build();
    }

    private SedeResponse convertirAResponse(Sede sede) {
        List<SedeResponse.ZonaResponse> zonas = sede.getZonas()
                .stream()
                .map(zona -> {
                    List<SedeResponse.AsientoResponse> asientos =
                            zona.getAsientos()
                                    .stream()
                                    .map(asiento ->
                                            new SedeResponse.AsientoResponse(
                                                    asiento.getCodigo(),
                                                    asiento.getFila(),
                                                    asiento.getNumero()
                                            ))
                                    .toList();

                    return new SedeResponse.ZonaResponse(
                            zona.getCodigo(),
                            zona.getNombre(),
                            zona.getPrecioBase(),
                            asientos
                    );
                })
                .toList();

        return new SedeResponse(
                sede.getId(),
                sede.getNombre(),
                sede.getDireccion(),
                sede.getCiudad(),
                zonas
        );
    }
}
