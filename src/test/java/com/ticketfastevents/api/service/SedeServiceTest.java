package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.SedeRequest;
import com.ticketfastevents.api.dto.SedeResponse;
import com.ticketfastevents.api.model.Sede;
import com.ticketfastevents.api.repository.SedeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SedeServiceTest {

    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private SedeService sedeService;

    @Test
    @DisplayName("Crear sede con datos válidos debe persistir y retornar SedeResponse")
    void crearSedeExitosa() {
        SedeRequest.AsientoRequest asiento = new SedeRequest.AsientoRequest("VIP-A1", "A", 1);
        SedeRequest.ZonaRequest zona = new SedeRequest.ZonaRequest("VIP", "Zona VIP", new BigDecimal("150.00"), List.of(asiento));
        SedeRequest request = new SedeRequest("Arena 1", "Costa Verde", "Lima", List.of(zona));

        when(sedeRepository.existsByNombreIgnoreCase("Arena 1")).thenReturn(false);

        Sede sedeGuardada = Sede.builder()
                .id("sede-123")
                .nombre("Arena 1")
                .direccion("Costa Verde")
                .ciudad("Lima")
                .zonas(List.of(Sede.Zona.builder()
                        .codigo("VIP")
                        .nombre("Zona VIP")
                        .precioBase(new BigDecimal("150.00"))
                        .asientos(List.of(Sede.Asiento.builder().codigo("VIP-A1").fila("A").numero(1).build()))
                        .build()))
                .build();

        when(sedeRepository.save(any(Sede.class))).thenReturn(sedeGuardada);

        SedeResponse response = sedeService.crear(request);

        assertNotNull(response);
        assertEquals("sede-123", response.id());
        assertEquals("Arena 1", response.nombre());
        assertEquals(1, response.zonas().size());
        assertEquals(1, response.zonas().get(0).asientos().size());
    }

    @Test
    @DisplayName("Crear sede con nombre duplicado debe lanzar ResponseStatusException 409")
    void crearSedeNombreDuplicado() {
        SedeRequest request = new SedeRequest("Estadio Nacional", "Av. Principal", "Lima", List.of());

        when(sedeRepository.existsByNombreIgnoreCase("Estadio Nacional")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> sedeService.crear(request));
    }

    @Test
    @DisplayName("Crear sede con zonas duplicadas debe lanzar ResponseStatusException 400")
    void crearSedeZonasDuplicadas() {
        SedeRequest.AsientoRequest a1 = new SedeRequest.AsientoRequest("VIP-1", "A", 1);
        SedeRequest.ZonaRequest z1 = new SedeRequest.ZonaRequest("VIP", "VIP 1", new BigDecimal("100"), List.of(a1));
        SedeRequest.ZonaRequest z2 = new SedeRequest.ZonaRequest("VIP", "VIP 2", new BigDecimal("100"), List.of(a1));

        SedeRequest request = new SedeRequest("Coliseo", "Av. 1", "Lima", List.of(z1, z2));

        when(sedeRepository.existsByNombreIgnoreCase("Coliseo")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> sedeService.crear(request));
    }

    @Test
    @DisplayName("Buscar sede por ID existente")
    void buscarPorIdExistente() {
        Sede sede = Sede.builder().id("sede-1").nombre("Arena").direccion("Dir").ciudad("Lima").zonas(List.of()).build();
        when(sedeRepository.findById("sede-1")).thenReturn(Optional.of(sede));

        SedeResponse response = sedeService.buscarPorId("sede-1");

        assertNotNull(response);
        assertEquals("sede-1", response.id());
    }

    @Test
    @DisplayName("Eliminar sede existente")
    void eliminarExistente() {
        when(sedeRepository.existsById("sede-1")).thenReturn(true);
        doNothing().when(sedeRepository).deleteById("sede-1");

        assertDoesNotThrow(() -> sedeService.eliminar("sede-1"));
        verify(sedeRepository, times(1)).deleteById("sede-1");
    }
}
