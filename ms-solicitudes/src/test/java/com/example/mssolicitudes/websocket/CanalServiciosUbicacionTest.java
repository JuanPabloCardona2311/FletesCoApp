package com.example.mssolicitudes.websocket;

import com.example.mssolicitudes.entity.Despachador;
import com.example.mssolicitudes.entity.Solicitud;
import com.example.mssolicitudes.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CanalServiciosUbicacionTest {

    private static final String UBICACION =
            "{\"tipo\":\"UBICACION_ACTUALIZADA\",\"conductorId\":7,\"latitud\":6.2442,\"longitud\":-75.5812,\"fecha\":\"2026-11-22T13:30:00\"}";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private SolicitudRepository solicitudRepository;
    private CanalUsuariosHandler canalUsuarios;
    private CanalServiciosHandler canal;

    @BeforeEach
    void preparar() {
        solicitudRepository = mock(SolicitudRepository.class);
        canalUsuarios = mock(CanalUsuariosHandler.class);
        canal = new CanalServiciosHandler(jsonMapper, solicitudRepository, canalUsuarios);
    }

    @Test
    void conViajeActivoSeEnviaSoloAlDespachadorDuenoConLosCamposDelDetalle() throws Exception {
        when(solicitudRepository.findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(eq(7L), anyCollection()))
                .thenReturn(Optional.of(Solicitud.builder().id(20L).estado(Solicitud.EstadoSolicitud.EN_CURSO)
                        .despachador(Despachador.builder().id(3L).build()).build()));

        canal.handleTextMessage(sesion("ms-perfil"), new TextMessage(UBICACION));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(canalUsuarios).enviarADespachador(eq(3L), json.capture());
        verify(canalUsuarios, never()).enviarAConductor(any(), anyString());
        verify(canalUsuarios, never()).enviarATodosLosConductores(anyString());
        JsonNode aviso = jsonMapper.readTree(json.getValue());
        assertThat(aviso.propertyNames())
                .containsExactly("tipo", "solicitudId", "conductorLat", "conductorLng", "ubicacionActualizadaEn");
        assertThat(aviso.get("solicitudId").asLong()).isEqualTo(20L);
        assertThat(aviso.get("conductorLat").decimalValue()).isEqualByComparingTo("6.2442");
        assertThat(aviso.get("conductorLng").decimalValue()).isEqualByComparingTo("-75.5812");
        assertThat(aviso.get("ubicacionActualizadaEn").asString()).isEqualTo("2026-11-22T13:30:00");
    }

    @Test
    void sinViajeActivoNoSeEnviaNada() throws Exception {
        when(solicitudRepository.findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(anyLong(), anyCollection()))
                .thenReturn(Optional.empty());

        canal.handleTextMessage(sesion("ms-perfil"), new TextMessage(UBICACION));

        verifyNoInteractions(canalUsuarios);
    }

    @Test
    void desdeOtroServicioSeIgnora() throws Exception {
        canal.handleTextMessage(sesion("ms-admin-reportes"), new TextMessage(UBICACION));

        verifyNoInteractions(solicitudRepository, canalUsuarios);
    }

    @Test
    void jsonIncompletoSeIgnoraSinExcepcion() {
        WebSocketSession perfil = sesion("ms-perfil");

        assertThatCode(() -> {
            canal.handleTextMessage(perfil, new TextMessage("{\"tipo\":\"UBICACION_ACTUALIZADA\",\"conductorId\":7}"));
            canal.handleTextMessage(perfil, new TextMessage("{\"tipo\":\"UBICACION_ACTUALIZADA\",\"conductorId\":\"siete\",\"latitud\":1,\"longitud\":2}"));
            canal.handleTextMessage(perfil, new TextMessage("no es json"));
        }).doesNotThrowAnyException();
        verifyNoInteractions(solicitudRepository, canalUsuarios);
    }

    private static WebSocketSession sesion(String servicio) {
        WebSocketSession sesion = mock(WebSocketSession.class);
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(TokenServicioInterceptor.ATRIBUTO_SERVICIO, servicio);
        when(sesion.getId()).thenReturn(servicio);
        when(sesion.getAttributes()).thenReturn(atributos);
        return sesion;
    }
}
