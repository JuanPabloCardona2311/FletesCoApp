package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.entity.Solicitud;
import com.example.mssolicitudes.repository.SolicitudRepository;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Canal interno /internal/ws/eventos: los otros servicios se conectan y reciben los
// eventos de solicitudes. Responde PING con PONG, y de ms-perfil acepta además la
// ubicación del conductor, que reenvía al despachador dueño del viaje.
@Component
public class CanalServiciosHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalServiciosHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO = 512 * 1024;
    private static final String SERVICIO_UBICACION = "ms-perfil";
    // Mismo criterio que construirDetalle: la ubicación solo se comparte con el viaje activo.
    private static final List<Solicitud.EstadoSolicitud> ESTADOS_VIAJE_ACTIVO =
            List.of(Solicitud.EstadoSolicitud.ACEPTADA, Solicitud.EstadoSolicitud.EN_CURSO);

    // Sesiones abiertas por id. Varios hilos pueden enviar a la vez (cada petición HTTP
    // publica su evento): el decorador serializa los envíos; sin él Tomcat lanza
    // IllegalStateException.
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final SolicitudRepository solicitudRepository;
    private final CanalUsuariosHandler canalUsuarios;

    public CanalServiciosHandler(ObjectMapper objectMapper, SolicitudRepository solicitudRepository,
            CanalUsuariosHandler canalUsuarios) {
        this.objectMapper = objectMapper;
        this.solicitudRepository = solicitudRepository;
        this.canalUsuarios = canalUsuarios;
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
            String tipo = json.path("tipo").asString();
            if ("PING".equals(tipo)) {
                WebSocketSession destino = sesiones.getOrDefault(session.getId(), session);
                destino.sendMessage(new TextMessage("{\"tipo\":\"PONG\"}"));
            } else if ("UBICACION_ACTUALIZADA".equals(tipo)) {
                reenviarUbicacion(session, json);
            }
        } catch (Exception ex) {
            log.debug("Mensaje ignorado en el canal de eventos: {}", ex.getMessage());
        }
    }

    // Reenvía la ubicación del conductor solo al despachador dueño de su viaje activo, con
    // los mismos nombres de campo que SolicitudDetalleResponse. El conductor no la recibe
    // de vuelta y ningún otro despachador recibe nada.
    private void reenviarUbicacion(WebSocketSession session, JsonNode json) {
        String servicio = nombreServicio(session);
        if (!SERVICIO_UBICACION.equals(servicio)) {
            log.warn("Ubicación ignorada: solo ms-perfil puede enviarla y llegó de {}", servicio);
            return;
        }
        JsonNode conductorId = json.get("conductorId");
        JsonNode latitud = json.get("latitud");
        JsonNode longitud = json.get("longitud");
        if (conductorId == null || !conductorId.isIntegralNumber()
                || latitud == null || !latitud.isNumber() || longitud == null || !longitud.isNumber()) {
            log.debug("Ubicación ignorada: faltan conductorId, latitud o longitud numéricos");
            return;
        }
        solicitudRepository.findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(
                        conductorId.asLong(), ESTADOS_VIAJE_ACTIVO)
                .filter(solicitud -> solicitud.getDespachador() != null)
                .ifPresent(solicitud -> {
                    JsonNode fecha = json.get("fecha");
                    Map<String, Object> aviso = new LinkedHashMap<>();
                    aviso.put("tipo", "UBICACION_ACTUALIZADA");
                    aviso.put("solicitudId", solicitud.getId());
                    aviso.put("conductorLat", latitud.decimalValue());
                    aviso.put("conductorLng", longitud.decimalValue());
                    aviso.put("ubicacionActualizadaEn", fecha != null && fecha.isString()
                            ? fecha.asString() : LocalDateTime.now().toString());
                    canalUsuarios.enviarADespachador(solicitud.getDespachador().getId(),
                            objectMapper.writeValueAsString(aviso));
                });
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
