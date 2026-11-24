package com.example.mssolicitudes.evento;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import com.example.mssolicitudes.websocket.CanalUsuariosHandler;
import com.example.mssolicitudes.websocket.JwtNavegadorInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DifusorEventosTest {

    private static final long DESPACHADOR_DUENO = 3L;
    private static final long OTRO_DESPACHADOR = 4L;
    private static final long CONDUCTOR_ASIGNADO = 7L;
    private static final long OTRO_CONDUCTOR = 8L;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private CanalServiciosHandler canalServicios;
    private DifusorEventos difusor;
    private WebSocketSession despachadorDueno;
    private WebSocketSession otroDespachador;
    private WebSocketSession conductorAsignado;
    private WebSocketSession otroConductor;

    @BeforeEach
    void preparar() {
        canalServicios = mock(CanalServiciosHandler.class);
        CanalUsuariosHandler canalUsuarios = new CanalUsuariosHandler(jsonMapper);
        difusor = new DifusorEventos(canalServicios, canalUsuarios, jsonMapper);
        despachadorDueno = sesion("d1", "DESPACHADOR", JwtNavegadorInterceptor.ATRIBUTO_DESPACHADOR_ID, DESPACHADOR_DUENO);
        otroDespachador = sesion("d2", "DESPACHADOR", JwtNavegadorInterceptor.ATRIBUTO_DESPACHADOR_ID, OTRO_DESPACHADOR);
        conductorAsignado = sesion("c1", "CONDUCTOR", JwtNavegadorInterceptor.ATRIBUTO_CONDUCTOR_ID, CONDUCTOR_ASIGNADO);
        otroConductor = sesion("c2", "CONDUCTOR", JwtNavegadorInterceptor.ATRIBUTO_CONDUCTOR_ID, OTRO_CONDUCTOR);
        for (WebSocketSession s : new WebSocketSession[] {despachadorDueno, otroDespachador, conductorAsignado, otroConductor}) {
            canalUsuarios.afterConnectionEstablished(s);
        }
    }

    @Test
    void unFalloAlDifundirNoSePropagaAlServicio() {
        doThrow(new IllegalStateException("socket roto")).when(canalServicios).difundir(anyString());

        assertThatCode(() -> difusor.difundir(evento(EventoSolicitud.SOLICITUD_CREADA, null))).doesNotThrowAnyException();
        verify(canalServicios).difundir(contains("\"tipo\":\"SOLICITUD_CREADA\""));
    }

    @Test
    void solicitudCreadaVaATodosLosConductoresYANingunDespachador() throws Exception {
        difusor.difundir(evento(EventoSolicitud.SOLICITUD_CREADA, null));

        verify(conductorAsignado).sendMessage(any(TextMessage.class));
        verify(otroConductor).sendMessage(any(TextMessage.class));
        verify(despachadorDueno, never()).sendMessage(any());
        verify(otroDespachador, never()).sendMessage(any());
    }

    @Test
    void solicitudAceptadaVaATodosLosConductoresYAlDespachadorDueno() throws Exception {
        difusor.difundir(evento(EventoSolicitud.SOLICITUD_ACEPTADA, CONDUCTOR_ASIGNADO));

        verify(conductorAsignado).sendMessage(any(TextMessage.class));
        verify(otroConductor).sendMessage(any(TextMessage.class));
        verify(despachadorDueno).sendMessage(any(TextMessage.class));
        verify(otroDespachador, never()).sendMessage(any());
    }

    @Test
    void viajeIniciadoSoloLlegaAlDespachadorDuenoYAlConductorAsignado() throws Exception {
        difusor.difundir(evento(EventoSolicitud.VIAJE_INICIADO, CONDUCTOR_ASIGNADO));

        verify(despachadorDueno).sendMessage(any(TextMessage.class));
        verify(conductorAsignado).sendMessage(any(TextMessage.class));
        verify(otroDespachador, never()).sendMessage(any());
        verify(otroConductor, never()).sendMessage(any());
    }

    @Test
    void elMensajeDelNavegadorSoloTieneTipoSolicitudIdYEstado() throws Exception {
        difusor.difundir(evento(EventoSolicitud.VIAJE_INICIADO, CONDUCTOR_ASIGNADO));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(despachadorDueno, times(1)).sendMessage(captor.capture());
        JsonNode json = jsonMapper.readTree(captor.getValue().getPayload());
        assertThat(json.propertyNames()).containsExactly("tipo", "solicitudId", "estado");
        assertThat(json.get("tipo").asString()).isEqualTo("VIAJE_INICIADO");
        assertThat(json.get("solicitudId").asLong()).isEqualTo(20L);
        assertThat(json.get("estado").asString()).isEqualTo("EN_CURSO");
    }

    private static EventoSolicitud evento(String tipo, Long conductorId) {
        String estado = switch (tipo) {
            case EventoSolicitud.SOLICITUD_CREADA -> "PUBLICADA";
            case EventoSolicitud.SOLICITUD_ACEPTADA -> "ACEPTADA";
            default -> "EN_CURSO";
        };
        return new EventoSolicitud(tipo, 20L, null, estado, DESPACHADOR_DUENO, conductorId, "2026-11-22T10:00:00Z");
    }

    private static WebSocketSession sesion(String id, String rol, String atributoId, Long valorId) {
        WebSocketSession sesion = mock(WebSocketSession.class);
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(JwtNavegadorInterceptor.ATRIBUTO_ROL, rol);
        atributos.put(atributoId, valorId);
        when(sesion.getId()).thenReturn(id);
        when(sesion.isOpen()).thenReturn(true);
        when(sesion.getAttributes()).thenReturn(atributos);
        return sesion;
    }
}
