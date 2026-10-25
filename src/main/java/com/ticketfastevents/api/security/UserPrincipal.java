package com.ticketfastevents.api.security;

import com.ticketfastevents.api.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final String id;
    private final String correo;
    private final String password;
    private final String nombres;
    private final String apellidos;
    private final String rol;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            String id,
            String correo,
            String password,
            String nombres,
            String apellidos,
            String rol,
            boolean activo,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.correo = correo;
        this.password = password;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.rol = rol;
        this.activo = activo;
        this.authorities = authorities;
    }

    public static UserPrincipal fromUsuario(Usuario usuario) {
        String rol = usuario.getRol() != null && !usuario.getRol().isBlank() ? usuario.getRol().trim().toUpperCase() : "ROLE_CLIENTE";
        if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol;
        }

        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(rol));
        if (rol.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority(rol.substring(5)));
        }

        return new UserPrincipal(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getPassword(),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol,
                usuario.isActivo(),
                authorities
        );
    }

    public String getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
