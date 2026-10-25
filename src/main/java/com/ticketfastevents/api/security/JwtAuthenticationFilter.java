package com.ticketfastevents.api.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerTrimmed = authHeader.trim();
        if (!headerTrimmed.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = headerTrimmed.substring(7).trim();
        // Limpiar prefijos redundantes (por ejemplo si el usuario ingresa 'Bearer <token>' en Swagger UI)
        while (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        if (!token.isBlank() && jwtService.esTokenValido(token)) {
            Claims claims = jwtService.extraerClaims(token);
            String correo = claims.getSubject();
            String usuarioId = claims.get("usuarioId", String.class);
            String rol = claims.get("rol", String.class);
            String nombres = claims.get("nombres", String.class);
            String apellidos = claims.get("apellidos", String.class);

            if (rol == null || rol.isBlank()) {
                rol = "ROLE_CLIENTE";
            } else {
                rol = rol.trim().toUpperCase();
                if (!rol.startsWith("ROLE_")) {
                    rol = "ROLE_" + rol;
                }
            }

            java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(rol));
            if (rol.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority(rol.substring(5)));
            }

            UserPrincipal principal = new UserPrincipal(
                    usuarioId,
                    correo,
                    null,
                    nombres,
                    apellidos,
                    rol,
                    true,
                    authorities
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
