package com.example.mssolicitudes.evento;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DifusorEventosTest {

    @Test
    void unFalloAlDifundirNoSePropagaAlServicio() {
        CanalServiciosHandler canal = mock(CanalServiciosHandler.class);
        doThrow(new IllegalStateException("socket roto")).when(canal).difundir(anyString());
        DifusorEventos difusor = new DifusorEventos(canal, JsonMapper.builder().build());
        EventoSolicitud evento = new EventoSolicitud(EventoSolicitud.SOLICITUD_CREADA, 20L, null, "PUBLICADA",
                3L, null, "2026-11-22T10:00:00Z");

        assertThatCode(() -> difusor.difundir(evento)).doesNotThrowAnyException();
        verify(canal).difundir(contains("\"tipo\":\"SOLICITUD_CREADA\""));
    }
}
