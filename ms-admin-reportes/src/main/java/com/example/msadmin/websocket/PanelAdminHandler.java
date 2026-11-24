package com.example.msadmin.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// WebSocket /api/admin/ws: el navegador del administrador recibe aquí los eventos de
// solicitudes que llegan de ms-solicitudes y el estado del enlace entre los dos servicios.
@Component
public class PanelAdminHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(PanelAdminHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO = 512 * 1024;

    // Sesiones abiertas por id, con el decorador que serializa los envíos de varios hilos.
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private volatile boolean enlaceConectado;

    public PanelAdminHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Al entrar, el admin recibe de inmediato el estado actual del enlace.
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession decorada = new ConcurrentWebSocketSessionDecorator(session, TIEMPO_ENVIO_MS, BUFFER_MAXIMO);
        sesiones.put(session.getId(), decorada);
        enviar(session.getId(), decorada, new TextMessage(estadoEnlace()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session.getId());
    }

    // El navegador solo envía PING para mantener viva la conexión.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            if ("PING".equals(objectMapper.readTree(message.getPayload()).path("tipo").asString())) {
                WebSocketSession destino = sesiones.get(session.getId());
                if (destino != null) {
                    enviar(session.getId(), destino, new TextMessage("{\"tipo\":\"PONG\"}"));
                }
            }
        } catch (Exception ex) {
            log.debug("Mensaje ignorado en el panel admin: {}", ex.getMessage());
        }
    }

    // Envía el JSON a todos los admins conectados y quita las sesiones que fallen.
    public void difundir(String json) {
        TextMessage mensaje = new TextMessage(json);
        sesiones.forEach((id, sesion) -> enviar(id, sesion, mensaje));
    }

    // Avisa a los admins solo cuando el enlace con ms-solicitudes cambia de estado.
    public void actualizarEnlace(boolean conectado) {
        if (enlaceConectado == conectado) {
            return;
        }
        enlaceConectado = conectado;
        difundir(estadoEnlace());
    }

    int sesionesActivas() {
        return sesiones.size();
    }

    private String estadoEnlace() {
        return "{\"tipo\":\"ENLACE_ESTADO\",\"conectado\":" + enlaceConectado + "}";
    }

    private void enviar(String id, WebSocketSession sesion, TextMessage mensaje) {
        try {
            sesion.sendMessage(mensaje);
        } catch (Exception ex) {
            log.debug("Se quita una sesión del panel admin que falló al enviar: {}", ex.getMessage());
            sesiones.remove(id);
            try {
                sesion.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (Exception ignorada) {
                // Ya estaba cerrada.
            }
        }
    }
}
