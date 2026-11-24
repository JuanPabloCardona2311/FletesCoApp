package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

// Autentica a los servicios que se conectan al canal interno. El token viaja en
// X-Token-Servicio y no en Authorization: así el JwtAuthenticationFilter no lo
// confunde con un usuario (lo buscaría en la tabla usuarios y fallaría).
@Component
public class TokenServicioInterceptor implements HandshakeInterceptor {

    public static final String HEADER_TOKEN = "X-Token-Servicio";
    public static final String ATRIBUTO_SERVICIO = "servicio";
    private static final Logger log = LoggerFactory.getLogger(TokenServicioInterceptor.class);

    private final JwtService jwtService;

    public TokenServicioInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Optional<String> servicio = jwtService.validarTokenServicio(request.getHeaders().getFirst(HEADER_TOKEN));
        if (servicio.isEmpty()) {
            log.warn("Conexión rechazada al canal de eventos: token de servicio ausente o inválido");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATRIBUTO_SERVICIO, servicio.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // La conexión y la desconexión se registran en CanalServiciosHandler.
    }
}
