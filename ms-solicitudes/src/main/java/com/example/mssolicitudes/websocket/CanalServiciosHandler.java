package com.example.mssolicitudes.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Canal interno /internal/ws/eventos: los otros servicios se conectan y reciben los
// eventos de solicitudes. Aquí los servicios solo escuchan; lo único que se responde
// es el PING con PONG.
@Component
public class CanalServiciosHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalServiciosHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO = 512 * 1024;

    // Sesiones abiertas por id. Varios hilos pueden enviar a la vez (cada petición HTTP
    // publica su evento): el decorador serializa los envíos; sin él Tomcat lanza
    // IllegalStateException.
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public CanalServiciosHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sesiones.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, TIEMPO_ENVIO_MS, BUFFER_MAXIMO));
        log.info("{} conectado al canal de eventos", nombreServicio(session));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session.getId());
        log.info("{} desconectado del canal de eventos ({})", nombreServicio(session), status.getCode());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            if ("PING".equals(json.path("tipo").asString())) {
                WebSocketSession destino = sesiones.getOrDefault(session.getId(), session);
                destino.sendMessage(new TextMessage("{\"tipo\":\"PONG\"}"));
            }
        } catch (Exception ex) {
            log.debug("Mensaje ignorado en el canal de eventos: {}", ex.getMessage());
        }
    }

    // Envía el JSON a todos los servicios conectados y quita las sesiones que fallen.
    public void difundir(String json) {
        TextMessage mensaje = new TextMessage(json);
        sesiones.forEach((id, sesion) -> {
            try {
                sesion.sendMessage(mensaje);
            } catch (Exception ex) {
                log.warn("Se quita del canal de eventos una sesión que falló al enviar: {}", ex.getMessage());
                sesiones.remove(id);
                cerrarSinError(sesion);
            }
        });
    }

    int sesionesActivas() {
        return sesiones.size();
    }

    private static String nombreServicio(WebSocketSession session) {
        Object nombre = session.getAttributes().get(TokenServicioInterceptor.ATRIBUTO_SERVICIO);
        return nombre != null ? nombre.toString() : "Servicio desconocido";
    }

    private static void cerrarSinError(WebSocketSession sesion) {
        try {
            sesion.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (Exception ignorada) {
            // Ya estaba cerrada.
        }
    }
}
