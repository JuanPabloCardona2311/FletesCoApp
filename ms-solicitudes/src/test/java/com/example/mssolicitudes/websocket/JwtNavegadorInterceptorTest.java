package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.entity.Conductor;
import com.example.mssolicitudes.entity.Usuario;
import com.example.mssolicitudes.repository.ConductorRepository;
import com.example.mssolicitudes.repository.DespachadorRepository;
import com.example.mssolicitudes.repository.UsuarioRepository;
import com.example.mssolicitudes.security.JwtService;
import com.example.mssolicitudes.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private UsuarioRepository usuarioRepository;
    private ConductorRepository conductorRepository;
    private JwtNavegadorInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Map<String, Object> atributos;

    @BeforeEach
    void preparar() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);
        usuarioRepository = mock(UsuarioRepository.class);
        conductorRepository = mock(ConductorRepository.class);
        interceptor = new JwtNavegadorInterceptor(jwtService, userDetailsService, usuarioRepository,
                mock(DespachadorRepository.class), conductorRepository);
        request = new MockHttpServletRequest("GET", "/api/solicitudes/ws");
        response = new MockHttpServletResponse();
        atributos = new HashMap<>();
        when(jwtService.extractUsername(TOKEN)).thenReturn(CORREO);
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }

    @Test
    void sinTokenRechazaCon401() {
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void administradorRechazaCon403() {
        conUsuario(Usuario.TipoUsuario.ADMINISTRADOR, true);

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void usuarioInactivoRechazaCon403() {
        conUsuario(Usuario.TipoUsuario.CONDUCTOR, false);

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void conductorActivoAceptaYGuardaSuConductorId() {
        conUsuario(Usuario.TipoUsuario.CONDUCTOR, true);
        when(conductorRepository.findByUsuarioId(5L)).thenReturn(Optional.of(Conductor.builder().id(70L).build()));

        assertThat(handshake()).isTrue();
        assertThat(atributos)
                .containsEntry(JwtNavegadorInterceptor.ATRIBUTO_ROL, "CONDUCTOR")
                .containsEntry(JwtNavegadorInterceptor.ATRIBUTO_CONDUCTOR_ID, 70L)
                .doesNotContainKey(JwtNavegadorInterceptor.ATRIBUTO_DESPACHADOR_ID);
    }

    @Test
    void conductorConCookieSinQueryAcepta() {
        conUsuario(Usuario.TipoUsuario.CONDUCTOR, true);
        request.setQueryString(null);
        request.addHeader("Cookie", "otra=valor; FLETESCO_TOKEN=" + TOKEN);

        assertThat(handshake()).isTrue();
        assertThat(atributos).containsEntry(JwtNavegadorInterceptor.ATRIBUTO_ROL, "CONDUCTOR");
    }

    private void conUsuario(Usuario.TipoUsuario tipo, boolean activo) {
        request.setQueryString("token=" + TOKEN);
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(User.withUsername(CORREO).password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name()))).disabled(!activo).build());
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(Usuario.builder().id(5L).email(CORREO)
                .tipoUsuario(tipo).estado(activo ? Usuario.EstadoUsuario.ACTIVO : Usuario.EstadoUsuario.INACTIVO).build()));
    }

    private boolean handshake() {
        return interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response), mock(WebSocketHandler.class), atributos);
    }
}
