package com.ticketfastevents.api.service;

import com.ticketfastevents.api.dto.AuthResponse;
import com.ticketfastevents.api.dto.LoginRequest;
import com.ticketfastevents.api.dto.UsuarioRequest;
import com.ticketfastevents.api.dto.UsuarioResponse;
import com.ticketfastevents.api.model.Usuario;
import com.ticketfastevents.api.repository.UsuarioRepository;
import com.ticketfastevents.api.security.JwtService;
import com.ticketfastevents.api.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || request.correo() == null || request.password() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo y la contraseña son obligatorios"
            );
        }

        String correo = request.correo().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciales inválidas"
                ));

        if (!usuario.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "La cuenta de usuario se encuentra inactiva"
            );
        }

        if (usuario.getPassword() == null || !passwordEncoder.matches(request.password().trim(), usuario.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas"
                );
        }

        String rol = usuario.getRol() != null && !usuario.getRol().isBlank() ? usuario.getRol() : "ROLE_CLIENTE";
        String token = jwtService.generarToken(
                usuario.getId(),
                usuario.getCorreo(),
                rol,
                usuario.getNombres(),
                usuario.getApellidos()
        );

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol,
                jwtService.getExpirationMs()
        );
    }

    public AuthResponse registrar(UsuarioRequest request) {
        UsuarioResponse usuarioResponse = usuarioService.registrar(request);

        String rol = usuarioResponse.rol() != null ? usuarioResponse.rol() : "ROLE_CLIENTE";
        String token = jwtService.generarToken(
                usuarioResponse.id(),
                usuarioResponse.correo(),
                rol,
                usuarioResponse.nombres(),
                usuarioResponse.apellidos()
        );

        return new AuthResponse(
                token,
                "Bearer",
                usuarioResponse.id(),
                usuarioResponse.correo(),
                usuarioResponse.nombres(),
                usuarioResponse.apellidos(),
                rol,
                jwtService.getExpirationMs()
        );
    }

    public UsuarioResponse obtenerPerfil(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        return usuarioService.obtenerPorId(principal.getId());
    }
}
