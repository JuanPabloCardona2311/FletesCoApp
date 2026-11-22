package com.example.mssolicitudes.websocket;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// WebSocket /api/solicitudes/ws para el navegador de conductores y despachadores. Solo
// avisa "algo cambió"; cada pantalla vuelve a pedir los datos por el REST de siempre.
@Component
public class CanalUsuariosHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalUsuariosHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO = 512 * 1024;

    private record Conexion(WebSocketSession sesion, String rol, Long despachadorId, Long conductorId) {
    }

    // Sesiones por id, y para cada despachador o conductor el conjunto de sus sesiones:
    // un mismo usuario puede tener varias pestañas abiertas.
    private final Map<String, Conexion> conexiones = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> porDespachador = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> porConductor = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public CanalUsuariosHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> atributos = session.getAttributes();
        Conexion conexion = new Conexion(
                new ConcurrentWebSocketSessionDecorator(session, TIEMPO_ENVIO_MS, BUFFER_MAXIMO),
                (String) atributos.get(JwtNavegadorInterceptor.ATRIBUTO_ROL),
                (Long) atributos.get(JwtNavegadorInterceptor.ATRIBUTO_DESPACHADOR_ID),
                (Long) atributos.get(JwtNavegadorInterceptor.ATRIBUTO_CONDUCTOR_ID));
        conexiones.put(session.getId(), conexion);
        indexar(porDespachador, conexion.despachadorId(), session.getId());
        indexar(porConductor, conexion.conductorId(), session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        quitar(session.getId());
    }

    // El navegador solo envía PING para mantener viva la conexión; lo demás se ignora.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            if ("PING".equals(objectMapper.readTree(message.getPayload()).path("tipo").asString())) {
                enviar(session.getId(), new TextMessage("{\"tipo\":\"PONG\"}"));
            }
        } catch (Exception ex) {
            log.debug("Mensaje ignorado en el canal de usuarios: {}", ex.getMessage());
        }
    }

    public void enviarATodosLosConductores(String json) {
        TextMessage mensaje = new TextMessage(json);
        conexiones.forEach((id, conexion) -> {
            if ("CONDUCTOR".equals(conexion.rol())) {
                enviar(id, mensaje);
            }
        });
    }

    public void enviarADespachador(Long despachadorId, String json) {
        enviarA(porDespachador, despachadorId, json);
    }

    public void enviarAConductor(Long conductorId, String json) {
        enviarA(porConductor, conductorId, json);
    }

    int sesionesActivas() {
        return conexiones.size();
    }

    private void enviarA(Map<Long, Set<String>> indice, Long id, String json) {
        if (id == null) {
            return;
        }
        Set<String> sesiones = indice.get(id);
        if (sesiones == null) {
            return;
        }
        TextMessage mensaje = new TextMessage(json);
        sesiones.forEach(sesionId -> enviar(sesionId, mensaje));
    }

    // Si un envío falla, se quita esa sesión y se sigue con las demás. Nunca lanza excepción.
    private void enviar(String sesionId, TextMessage mensaje) {
        Conexion conexion = conexiones.get(sesionId);
        if (conexion == null) {
            return;
        }
        try {
            conexion.sesion().sendMessage(mensaje);
        } catch (Exception ex) {
            log.debug("Se quita del canal de usuarios una sesión que falló al enviar: {}", ex.getMessage());
            quitar(sesionId);
            try {
                conexion.sesion().close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (Exception ignorada) {
                // Ya estaba cerrada.
            }
        }
    }

    private void quitar(String sesionId) {
        Conexion conexion = conexiones.remove(sesionId);
        if (conexion == null) {
            return;
        }
        desindexar(porDespachador, conexion.despachadorId(), sesionId);
        desindexar(porConductor, conexion.conductorId(), sesionId);
    }

    private static void indexar(Map<Long, Set<String>> indice, Long id, String sesionId) {
        if (id != null) {
            indice.computeIfAbsent(id, clave -> ConcurrentHashMap.newKeySet()).add(sesionId);
        }
    }

    private static void desindexar(Map<Long, Set<String>> indice, Long id, String sesionId) {
        if (id == null) {
            return;
        }
        indice.computeIfPresent(id, (clave, sesiones) -> {
            sesiones.remove(sesionId);
            return sesiones.isEmpty() ? null : sesiones;
        });
    }
}
