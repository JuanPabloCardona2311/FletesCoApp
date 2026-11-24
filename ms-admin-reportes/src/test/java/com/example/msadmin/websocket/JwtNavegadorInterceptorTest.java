package com.example.msadmin.websocket;

import com.example.msadmin.security.JwtService;
import com.example.msadmin.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtNavegadorInterceptorTest {

    // Valor ficticio: JwtService está simulado y no valida firmas reales.
    private static final String TOKEN = "token-de-prueba";
    private static final String CORREO = "usuario@ejemplo.test";

    private JwtService jwtService;
    private UserDetailsServiceImpl userDetailsService;
    private JwtNavegadorInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void preparar() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);
        interceptor = new JwtNavegadorInterceptor(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/admin/ws");
        response = new MockHttpServletResponse();
        when(jwtService.extractUsername(TOKEN)).thenReturn(CORREO);
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }

    @Test
    void sinTokenRechazaCon401() {
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void tokenDeDespachadorRechazaCon403() {
        conToken(usuario("ROLE_DESPACHADOR", true));

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void administradorInactivoRechazaCon403() {
        conToken(usuario("ROLE_ADMINISTRADOR", false));

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void administradorActivoAcepta() {
        conToken(usuario("ROLE_ADMINISTRADOR", true));

        assertThat(handshake()).isTrue();
    }

    @Test
    void tokenEnLaUrlYaNoSeAcepta() {
        conToken(usuario("ROLE_ADMINISTRADOR", true));
        request.removeHeader("Cookie");
        request.setQueryString("token=" + TOKEN);

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    private void conToken(UserDetails usuario) {
        request.addHeader("Cookie", "otra=valor; FLETESCO_TOKEN=" + TOKEN);
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(usuario);
    }

    private static UserDetails usuario(String rol, boolean activo) {
        return User.withUsername(CORREO).password("x")
                .authorities(List.of(new SimpleGrantedAuthority(rol))).disabled(!activo).build();
    }

    private boolean handshake() {
        Map<String, Object> atributos = new HashMap<>();
        return interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response), mock(WebSocketHandler.class), atributos);
    }
}
