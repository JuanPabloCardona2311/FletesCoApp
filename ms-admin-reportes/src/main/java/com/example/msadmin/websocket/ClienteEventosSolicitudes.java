package com.example.msadmin.websocket;

import com.example.msadmin.security.JwtService;
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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

// Cliente del canal de eventos de ms-solicitudes. Reenvía cada evento al panel del
// administrador. Si ms-solicitudes no está, este servicio arranca y funciona igual
// (REST y GraphQL leen la base de datos) y se reconecta solo.
@Component
public class ClienteEventosSolicitudes extends TextWebSocketHandler {

    static final Duration ESPERA_INICIAL = Duration.ofSeconds(2);
    static final Duration ESPERA_MAXIMA = Duration.ofSeconds(30);
    static final Duration INTERVALO_PING = Duration.ofSeconds(25);
    private static final String HEADER_TOKEN = "X-Token-Servicio";
    private static final TextMessage PING = new TextMessage("{\"tipo\":\"PING\"}");
    private static final String RESINCRONIZAR = "{\"tipo\":\"RESINCRONIZAR\"}";
    private static final Logger log = LoggerFactory.getLogger(ClienteEventosSolicitudes.class);

    private final PanelAdminHandler panel;
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

    public ClienteEventosSolicitudes(PanelAdminHandler panel, JwtService jwtService, ObjectMapper objectMapper,
            @Qualifier("programadorEventos") TaskScheduler scheduler, WebSocketClient clienteWebSocket,
            @Value("${app.eventos.solicitudes-url:ws://localhost:8083/internal/ws/eventos}") String url) {
        this.panel = panel;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
        this.clienteWebSocket = clienteWebSocket;
        this.url = URI.create(url);
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

    // Conectado: reinicia la espera, empieza el PING y pide al panel que se resincronice
    // con la base de datos por lo que pudo pasar mientras no había enlace.
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
        panel.actualizarEnlace(true);
        panel.difundir(RESINCRONIZAR);
    }

    // Reenvía cada evento al panel tal como llega; los PONG se ignoran.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String json = message.getPayload();
        try {
            if ("PONG".equals(objectMapper.readTree(json).path("tipo").asString())) {
                return;
            }
        } catch (Exception ex) {
            log.debug("Mensaje no válido desde ms-solicitudes: {}", ex.getMessage());
            return;
        }
        panel.difundir(json);
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
        WebSocketSession anterior = sesion;
        sesion = null;
        cancelarPing();
        if (anterior != null) {
            panel.actualizarEnlace(false);
        }
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
