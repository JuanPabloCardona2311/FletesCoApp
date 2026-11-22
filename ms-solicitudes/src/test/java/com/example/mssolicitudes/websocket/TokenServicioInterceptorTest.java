package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TokenServicioInterceptorTest {

    // Clave ficticia, solo para pruebas.
    private static final String CLAVE_PRUEBA = "clave-de-prueba-solo-para-tests-0123456789";

    private JwtService jwtService;
    private TokenServicioInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Map<String, Object> atributos;

    @BeforeEach
    void preparar() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", CLAVE_PRUEBA);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        interceptor = new TokenServicioInterceptor(jwtService);
        request = new MockHttpServletRequest("GET", "/internal/ws/eventos");
        response = new MockHttpServletResponse();
        atributos = new HashMap<>();
    }

    @Test
    void sinHeaderRechazaCon401() {
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void tokenDeUsuarioNormalRechazaCon401() {
        String tokenUsuario = jwtService.generateToken(User.withUsername("despachador@ejemplo.test")
                .password("x").authorities(List.of()).build());
        request.addHeader(TokenServicioInterceptor.HEADER_TOKEN, tokenUsuario);

        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void tokenDeServicioValidoAcepta() {
        String tokenServicio = Jwts.builder().subject("ms-admin-reportes").claim("tipo", "SERVICIO")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(CLAVE_PRUEBA.getBytes(StandardCharsets.UTF_8))).compact();
        request.addHeader(TokenServicioInterceptor.HEADER_TOKEN, tokenServicio);

        assertThat(handshake()).isTrue();
        assertThat(atributos).containsEntry(TokenServicioInterceptor.ATRIBUTO_SERVICIO, "ms-admin-reportes");
    }

    private boolean handshake() {
        return interceptor.beforeHandshake(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response),
                mock(org.springframework.web.socket.WebSocketHandler.class), atributos);
    }
}
