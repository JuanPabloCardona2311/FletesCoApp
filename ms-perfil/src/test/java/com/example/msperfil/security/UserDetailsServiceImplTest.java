package com.example.msperfil.security;

import com.example.msperfil.entity.Usuario;
import com.example.msperfil.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        userDetailsService = new UserDetailsServiceImpl(usuarioRepository);
    }

    @Test
    void usuarioActivoQuedaHabilitadoConSuRol() {
        when(usuarioRepository.findByEmail("activo@fletesco.test"))
                .thenReturn(Optional.of(usuario("activo@fletesco.test", Usuario.EstadoUsuario.ACTIVO)));

        UserDetails detalles = userDetailsService.loadUserByUsername("activo@fletesco.test");

        assertThat(detalles.isEnabled()).isTrue();
        assertThat(detalles.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CONDUCTOR");
    }

    @Test
    void usuarioInactivoQuedaDeshabilitado() {
        when(usuarioRepository.findByEmail("inactivo@fletesco.test"))
                .thenReturn(Optional.of(usuario("inactivo@fletesco.test", Usuario.EstadoUsuario.INACTIVO)));

        UserDetails detalles = userDetailsService.loadUserByUsername("inactivo@fletesco.test");

        assertThat(detalles.isEnabled()).isFalse();
    }

    @Test
    void emailInexistenteLanzaUsernameNotFound() {
        when(usuarioRepository.findByEmail("nadie@fletesco.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nadie@fletesco.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static Usuario usuario(String email, Usuario.EstadoUsuario estado) {
        return Usuario.builder()
                .email(email)
                .passwordHash("hash-de-prueba")
                .tipoUsuario(Usuario.TipoUsuario.CONDUCTOR)
                .estado(estado)
                .build();
    }
}
