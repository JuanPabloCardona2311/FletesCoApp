package com.example.msperfil.websocket;

import com.example.msperfil.security.JwtService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

// Cliente del canal de eventos de ms-solicitudes: le envía la ubicación de cada conductor
// para que llegue en vivo al despachador. Si ms-solicitudes no está, este servicio arranca
// y guarda las ubicaciones igual (el despachador las ve con el refresco de respaldo) y se
// reconecta solo. Mismo esquema que ClienteEventosSolicitudes de ms-admin-reportes.
@Component
public class PublicadorUbicacion extends TextWebSocketHandler {

    static final Duration ESPERA_INICIAL = Duration.ofSeconds(2);
    static final Duration ESPERA_MAXIMA = Duration.ofSeconds(30);
    static final Duration INTERVALO_PING = Duration.ofSeconds(25);
    private static final String HEADER_TOKEN = "X-Token-Servicio";
    private static final TextMessage PING = new TextMessage("{\"tipo\":\"PING\"}");
    private static final Logger log = LoggerFactory.getLogger(PublicadorUbicacion.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler scheduler;
    private final WebSocketClient clienteWebSocket;
    private final URI url;

    private final AtomicBoolean reintentoPendiente = new AtomicBoolean(false);
    private volatile Duration espera = ESPERA_INICIAL;
    private volatile WebSocketSession sesion;
    private volatile ScheduledFuture<?> tareaPing;
    private volatile boolean detenido;

    public PublicadorUbicacion(JwtService jwtService, ObjectMapper objectMapper,
            @Qualifier("programadorEventos") TaskScheduler scheduler, WebSocketClient clienteWebSocket,
            @Value("${app.eventos.solicitudes-url:ws://localhost:8083/internal/ws/eventos}") String url) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
        this.clienteWebSocket = clienteWebSocket;
        this.url = URI.create(url);
    }

    // Envía la ubicación si hay conexión. Sin conexión no hace nada: se llama cada 20 s por
    // conductor, por eso el log es debug y no warn.
    public void publicar(Long conductorId, BigDecimal latitud, BigDecimal longitud, LocalDateTime fecha) {
        WebSocketSession actual = sesion;
        if (actual == null || !actual.isOpen()) {
            log.debug("Sin conexión con ms-solicitudes: no se publica la ubicación del conductor {}", conductorId);
            return;
        }
        try {
            Map<String, Object> mensaje = new LinkedHashMap<>();
            mensaje.put("tipo", "UBICACION_ACTUALIZADA");
            mensaje.put("conductorId", conductorId);
            mensaje.put("latitud", latitud);
            mensaje.put("longitud", longitud);
            mensaje.put("fecha", fecha != null ? fecha.toString() : null);
            actual.sendMessage(new TextMessage(objectMapper.writeValueAsString(mensaje)));
        } catch (Exception ex) {
            log.debug("No se pudo enviar la ubicación del conductor {}: {}", conductorId, ex.getMessage());
        }
    }

    // Arranca cuando la app ya está lista y en otro hilo: no bloquea el arranque.
    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        scheduler.schedule(this::conectar, Instant.now());
    }

    // Abre la conexión con un token de servicio nuevo; si falla, programa un reintento.
    void conectar() {
        if (detenido) {
            return;
        }
        try {
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add(HEADER_TOKEN, jwtService.generarTokenServicio());
            clienteWebSocket.execute(this, headers, url).whenComplete((abierta, error) -> {
                if (error != null) {
                    log.debug("No se pudo conectar con ms-solicitudes: {}", error.getMessage());
                    programarReintento();
                }
            });
        } catch (Exception ex) {
            log.debug("No se pudo conectar con ms-solicitudes: {}", ex.getMessage());
            programarReintento();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (detenido) {
            cerrarSinError(session);
            return;
        }
        sesion = new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024);
        espera = ESPERA_INICIAL;
        log.info("Conectado a ms-solicitudes");
        tareaPing = scheduler.scheduleAtFixedRate(this::enviarPing, Instant.now().plus(INTERVALO_PING), INTERVALO_PING);
    }

    // ms-solicitudes solo responde PONG; este servicio no espera otros mensajes.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Nada que hacer.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        manejarDesconexion();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Error en la conexión con ms-solicitudes: {}", exception.getMessage());
        cerrarSinError(session);
        manejarDesconexion();
    }

    // Al apagar la app: cierra la sesión y deja de reconectar.
    @PreDestroy
    public void detener() {
        detenido = true;
        cancelarPing();
        WebSocketSession actual = sesion;
        sesion = null;
        if (actual != null) {
            cerrarSinError(actual);
        }
    }

    private void manejarDesconexion() {
        sesion = null;
        cancelarPing();
        programarReintento();
    }

    // Espera creciente 2 s, 4 s, 8 s... hasta 30 s. Solo un reintento pendiente a la vez.
    void programarReintento() {
        if (detenido || !reintentoPendiente.compareAndSet(false, true)) {
            return;
        }
        Duration demora = espera;
        Duration siguiente = demora.multipliedBy(2);
        espera = siguiente.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : siguiente;
        log.warn("Sin conexión con ms-solicitudes, reintento en {} s", demora.toSeconds());
        scheduler.schedule(() -> {
            reintentoPendiente.set(false);
            conectar();
        }, Instant.now().plus(demora));
    }

    private void enviarPing() {
        WebSocketSession actual = sesion;
        if (actual == null || !actual.isOpen()) {
            return;
        }
        try {
            actual.sendMessage(PING);
        } catch (Exception ex) {
            log.debug("No se pudo enviar PING a ms-solicitudes: {}", ex.getMessage());
        }
    }

    private void cancelarPing() {
        ScheduledFuture<?> tarea = tareaPing;
        tareaPing = null;
        if (tarea != null) {
            tarea.cancel(false);
        }
    }

    private static void cerrarSinError(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignorada) {
            // Ya estaba cerrada.
        }
    }
}
