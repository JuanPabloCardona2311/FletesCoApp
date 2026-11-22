package com.example.msadmin.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PanelAdminHandlerTest {

    @Test
    void difundirQuitaLaSesionQueFallaYSigueConLasDemas() throws Exception {
        PanelAdminHandler panel = new PanelAdminHandler(JsonMapper.builder().build());
        WebSocketSession sana = sesion("sana");
        WebSocketSession fallida = sesion("fallida");
        // La primera vez (estado del enlace al conectar) funciona; después falla.
        doNothing().doThrow(new IOException("conexión rota")).when(fallida).sendMessage(any(WebSocketMessage.class));
        panel.afterConnectionEstablished(sana);
        panel.afterConnectionEstablished(fallida);

        panel.difundir("{\"tipo\":\"SOLICITUD_CREADA\",\"solicitudId\":1}");
        panel.difundir("{\"tipo\":\"SOLICITUD_CREADA\",\"solicitudId\":2}");

        assertThat(panel.sesionesActivas()).isEqualTo(1);
        verify(sana, times(3)).sendMessage(any(TextMessage.class));
        verify(fallida, times(2)).sendMessage(any(TextMessage.class));
    }

    private static WebSocketSession sesion(String id) {
        WebSocketSession sesion = mock(WebSocketSession.class);
        when(sesion.getId()).thenReturn(id);
        when(sesion.isOpen()).thenReturn(true);
        return sesion;
    }
}
