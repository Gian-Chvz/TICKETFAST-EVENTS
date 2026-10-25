package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse registrar(UsuarioRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario registrado con ese correo"
            );
        }

        String passwordHasheada = request.password() != null && !request.password().isBlank()
                ? passwordEncoder.encode(request.password().trim())
                : null;

        String rol = request.rol() != null && !request.rol().isBlank()
                ? request.rol().trim().toUpperCase(Locale.ROOT)
                : "ROLE_CLIENTE";

        if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol;
        }

        Usuario usuario = Usuario.builder()
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(correo)
                .password(passwordHasheada)
                .rol(rol)
                .telefono(request.telefono() == null
                        ? null
                        : request.telefono().trim())
                .build();

        return convertirAResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public UsuarioResponse obtenerPorId(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un usuario con el ID indicado"
                ));

        return convertirAResponse(usuario);
    }

    public UsuarioResponse convertirAResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol() != null ? usuario.getRol() : "ROLE_CLIENTE",
                usuario.getFechaRegistro(),
                usuario.isActivo()
        );
    }
}
