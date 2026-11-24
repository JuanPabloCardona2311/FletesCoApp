package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.entity.Usuario;
import com.example.mssolicitudes.repository.ConductorRepository;
import com.example.mssolicitudes.repository.DespachadorRepository;
import com.example.mssolicitudes.repository.UsuarioRepository;
import com.example.mssolicitudes.security.JwtService;
import com.example.mssolicitudes.security.UserDetailsServiceImpl;
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

// Autentica el WebSocket de conductores y despachadores. El navegador no puede enviar
// headers al abrir un WebSocket: el token llega en la cookie HttpOnly FLETESCO_TOKEN o, por
// compatibilidad, como ?token=... Nunca se escribe en el log. Al conectar se guarda el rol
// y el id de despachador o conductor, para saber a quién enviarle cada evento.
@Component
public class JwtNavegadorInterceptor implements HandshakeInterceptor {

    private static final String NOMBRE_COOKIE = "FLETESCO_TOKEN";

    public static final String ATRIBUTO_ROL = "rol";
    public static final String ATRIBUTO_DESPACHADOR_ID = "despachadorId";
    public static final String ATRIBUTO_CONDUCTOR_ID = "conductorId";
    private static final Logger log = LoggerFactory.getLogger(JwtNavegadorInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final DespachadorRepository despachadorRepository;
    private final ConductorRepository conductorRepository;

    public JwtNavegadorInterceptor(JwtService jwtService, UserDetailsServiceImpl userDetailsService,
            UsuarioRepository usuarioRepository, DespachadorRepository despachadorRepository,
            ConductorRepository conductorRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.despachadorRepository = despachadorRepository;
        this.conductorRepository = conductorRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extraerToken(request);
        if (token == null) {
            return rechazar(response, HttpStatus.UNAUTHORIZED);
        }
        try {
            UserDetails detalles = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
            if (!jwtService.isTokenValid(token, detalles)) {
                return rechazar(response, HttpStatus.UNAUTHORIZED);
            }
            Usuario usuario = usuarioRepository.findByEmail(detalles.getUsername()).orElse(null);
            Usuario.TipoUsuario rol = usuario != null ? usuario.getTipoUsuario() : null;
            if (!detalles.isEnabled()
                    || (rol != Usuario.TipoUsuario.DESPACHADOR && rol != Usuario.TipoUsuario.CONDUCTOR)) {
                log.debug("WebSocket de solicitudes rechazado para un usuario sin acceso");
                return rechazar(response, HttpStatus.FORBIDDEN);
            }
            attributes.put(ATRIBUTO_ROL, rol.name());
            if (rol == Usuario.TipoUsuario.DESPACHADOR) {
                despachadorRepository.findByUsuarioId(usuario.getId())
                        .ifPresent(despachador -> attributes.put(ATRIBUTO_DESPACHADOR_ID, despachador.getId()));
            } else {
                conductorRepository.findByUsuarioId(usuario.getId())
                        .ifPresent(conductor -> attributes.put(ATRIBUTO_CONDUCTOR_ID, conductor.getId()));
            }
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
