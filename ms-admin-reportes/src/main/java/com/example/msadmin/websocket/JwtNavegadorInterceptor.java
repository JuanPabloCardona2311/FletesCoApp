package com.example.msadmin.websocket;

import com.example.msadmin.security.JwtService;
import com.example.msadmin.security.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

// Autentica el WebSocket del panel admin. El navegador no puede enviar headers al abrir
// un WebSocket: el token llega en la cookie HttpOnly FLETESCO_TOKEN o, por compatibilidad,
// como ?token=... Nunca se escribe en el log.
@Component
public class JwtNavegadorInterceptor implements HandshakeInterceptor {

    private static final String NOMBRE_COOKIE = "FLETESCO_TOKEN";

    private static final Logger log = LoggerFactory.getLogger(JwtNavegadorInterceptor.class);
    private static final String ROL_ADMIN = "ROLE_ADMINISTRADOR";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtNavegadorInterceptor(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extraerToken(request);
        if (token == null) {
            return rechazar(response, HttpStatus.UNAUTHORIZED);
        }
        try {
            UserDetails usuario = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
            if (!jwtService.isTokenValid(token, usuario)) {
                return rechazar(response, HttpStatus.UNAUTHORIZED);
            }
            boolean esAdmin = usuario.getAuthorities().stream()
                    .anyMatch(autoridad -> ROL_ADMIN.equals(autoridad.getAuthority()));
            if (!usuario.isEnabled() || !esAdmin) {
                log.debug("WebSocket del panel rechazado para un usuario sin acceso de administrador");
                return rechazar(response, HttpStatus.FORBIDDEN);
            }
            attributes.put("usuario", usuario.getUsername());
            return true;
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
            return rechazar(response, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Nada que hacer después del handshake.
    }

    // Token del handshake: primero la cookie HttpOnly FLETESCO_TOKEN (el navegador la envía
    // sola al abrir el WebSocket); si no viene, ?token= como antes (compatibilidad).
    private static String extraerToken(ServerHttpRequest request) {
        String deCookie = tokenDeCookie(request);
        if (deCookie != null) {
            return deCookie;
        }
        String valor = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static String tokenDeCookie(ServerHttpRequest request) {
        List<String> cabeceras = request.getHeaders().get(HttpHeaders.COOKIE);
        if (cabeceras == null) {
            return null;
        }
        for (String cabecera : cabeceras) {
            for (String par : cabecera.split(";")) {
                String[] partes = par.trim().split("=", 2);
                if (partes.length == 2 && NOMBRE_COOKIE.equals(partes[0]) && !partes[1].isBlank()) {
                    return partes[1];
                }
            }
        }
        return null;
    }

    private static boolean rechazar(ServerHttpResponse response, HttpStatus estado) {
        response.setStatusCode(estado);
        return false;
    }
}
