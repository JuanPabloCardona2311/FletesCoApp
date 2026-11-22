package com.example.msadmin.websocket;

import com.example.msadmin.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteEventosSolicitudesTest {

    @Test
    void alCerrarseProgramaReintentosConEsperaCrecienteHasta30Segundos() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        WebSocketClient clienteWebSocket = mock(WebSocketClient.class);
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generarTokenServicio()).thenReturn("token-de-prueba");
        // ms-solicitudes sigue caído: cada intento de conexión falla.
        when(clienteWebSocket.execute(any(), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(CompletableFuture.failedFuture(new IOException("conexión rechazada")));
        ClienteEventosSolicitudes cliente = new ClienteEventosSolicitudes(mock(PanelAdminHandler.class), jwtService,
                JsonMapper.builder().build(), scheduler, clienteWebSocket, "ws://localhost:8083/internal/ws/eventos");

        cliente.afterConnectionClosed(mock(WebSocketSession.class), CloseStatus.GOING_AWAY);

        List<Long> esperas = new ArrayList<>();
        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> cuando = ArgumentCaptor.forClass(Instant.class);
        for (int intento = 1; intento <= 6; intento++) {
            verify(scheduler, times(intento)).schedule(tarea.capture(), cuando.capture());
            esperas.add(Math.round(Duration.between(Instant.now(), cuando.getValue()).toMillis() / 1000.0));
            // Se ejecuta el reintento: vuelve a fallar y programa el siguiente.
            tarea.getValue().run();
        }

        assertThat(esperas).containsExactly(2L, 4L, 8L, 16L, 30L, 30L);
        verify(clienteWebSocket, times(6)).execute(any(), any(WebSocketHttpHeaders.class), any(URI.class));
    }
}
