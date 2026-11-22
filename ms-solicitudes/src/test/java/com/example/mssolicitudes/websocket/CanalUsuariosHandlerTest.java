package com.example.mssolicitudes.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanalUsuariosHandlerTest {

    @Test
    void dosPestanasDelMismoDespachadorRecibenYAlCerrarUnaLaOtraSigue() throws Exception {
        CanalUsuariosHandler canal = new CanalUsuariosHandler(JsonMapper.builder().build());
        WebSocketSession pestana1 = sesionDespachador("p1", 3L);
        WebSocketSession pestana2 = sesionDespachador("p2", 3L);
        canal.afterConnectionEstablished(pestana1);
        canal.afterConnectionEstablished(pestana2);

        canal.enviarADespachador(3L, "{\"tipo\":\"SOLICITUD_ACEPTADA\",\"solicitudId\":1,\"estado\":\"ACEPTADA\"}");
        verify(pestana1, times(1)).sendMessage(any(TextMessage.class));
        verify(pestana2, times(1)).sendMessage(any(TextMessage.class));

        canal.afterConnectionClosed(pestana1, CloseStatus.NORMAL);
        canal.enviarADespachador(3L, "{\"tipo\":\"VIAJE_INICIADO\",\"solicitudId\":1,\"estado\":\"EN_CURSO\"}");

        verify(pestana1, times(1)).sendMessage(any(TextMessage.class));
        verify(pestana2, times(2)).sendMessage(any(TextMessage.class));
        assertThat(canal.sesionesActivas()).isEqualTo(1);
    }

    private static WebSocketSession sesionDespachador(String id, Long despachadorId) {
        WebSocketSession sesion = mock(WebSocketSession.class);
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(JwtNavegadorInterceptor.ATRIBUTO_ROL, "DESPACHADOR");
        atributos.put(JwtNavegadorInterceptor.ATRIBUTO_DESPACHADOR_ID, despachadorId);
        when(sesion.getId()).thenReturn(id);
        when(sesion.isOpen()).thenReturn(true);
        when(sesion.getAttributes()).thenReturn(atributos);
        return sesion;
    }
}
