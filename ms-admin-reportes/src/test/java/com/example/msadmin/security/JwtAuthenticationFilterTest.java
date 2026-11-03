package com.example.msadmin.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    // Valor ficticio: el JwtService está simulado y nunca valida firmas reales.
    private static final String TOKEN = "token-de-prueba";

    private JwtService jwtService;
    private UserDetailsServiceImpl userDetailsService;
    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void preparar() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);
        filtro = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioActivoConTokenValidoQuedaAutenticado() throws Exception {
        prepararToken("activo@fletesco.test", usuario("activo@fletesco.test", false));

        filtro.doFilter(request, response, filterChain);

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getName()).isEqualTo("activo@fletesco.test");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usuarioInactivoConTokenValidoNoQuedaAutenticadoYLaCadenaContinua() throws Exception {
        prepararToken("inactivo@fletesco.test", usuario("inactivo@fletesco.test", true));

        filtro.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void sinHeaderAuthorizationNoAutentica() throws Exception {
        filtro.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    private void prepararToken(String email, UserDetails detalles) {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(detalles);
        when(jwtService.isTokenValid(TOKEN, detalles)).thenReturn(true);
    }

    private static UserDetails usuario(String email, boolean deshabilitado) {
        return User.builder()
                .username(email)
                .password("hash-de-prueba")
                .authorities("ROLE_CONDUCTOR")
                .disabled(deshabilitado)
                .build();
    }
}
