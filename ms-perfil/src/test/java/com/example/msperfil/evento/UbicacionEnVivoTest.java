package com.example.msperfil.evento;

import com.example.msperfil.dto.request.UbicacionConductorRequest;
import com.example.msperfil.entity.Conductor;
import com.example.msperfil.entity.Usuario;
import com.example.msperfil.repository.ConductorRepository;
import com.example.msperfil.repository.DespachadorRepository;
import com.example.msperfil.repository.SolicitudRepository;
import com.example.msperfil.repository.UsuarioRepository;
import com.example.msperfil.repository.VehiculoRepository;
import com.example.msperfil.security.JwtService;
import com.example.msperfil.service.impl.PerfilServiceImpl;
import com.example.msperfil.websocket.PublicadorUbicacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.client.WebSocketClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UbicacionEnVivoTest {

    private static final String CORREO = "conductor@ejemplo.test";

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void actualizarUbicacionPublicaElEventoConConductorYCoordenadas() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ConductorRepository conductorRepository = mock(ConductorRepository.class);
        SolicitudRepository solicitudRepository = mock(SolicitudRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PerfilServiceImpl servicio = new PerfilServiceImpl(usuarioRepository, conductorRepository,
                mock(DespachadorRepository.class), mock(VehiculoRepository.class), solicitudRepository, publisher);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(Usuario.builder().id(2L).email(CORREO)
                .tipoUsuario(Usuario.TipoUsuario.CONDUCTOR).build()));
        when(conductorRepository.findByUsuarioId(2L)).thenReturn(Optional.of(Conductor.builder().id(7L).build()));
        when(solicitudRepository.existsByConductorIdAndEstadoIn(anyLong(), any())).thenReturn(true);

        servicio.actualizarUbicacionConductor(new UbicacionConductorRequest(new BigDecimal("6.2442"), new BigDecimal("-75.5812")));

        ArgumentCaptor<UbicacionActualizada> captor = ArgumentCaptor.forClass(UbicacionActualizada.class);
        verify(publisher).publishEvent(captor.capture());
        UbicacionActualizada evento = captor.getValue();
        assertThat(evento.conductorId()).isEqualTo(7L);
        assertThat(evento.latitud()).isEqualByComparingTo("6.2442");
        assertThat(evento.longitud()).isEqualByComparingTo("-75.5812");
        assertThat(evento.fecha()).isNotNull();
    }

    @Test
    void unFalloAlPublicarNoSePropagaAlPutDelConductor() {
        PublicadorUbicacion publicador = mock(PublicadorUbicacion.class);
        doThrow(new IllegalStateException("socket roto")).when(publicador).publicar(any(), any(), any(), any());
        OyenteUbicacion oyente = new OyenteUbicacion(publicador);

        assertThatCode(() -> oyente.alGuardarUbicacion(new UbicacionActualizada(7L, BigDecimal.ONE, BigDecimal.TEN,
                LocalDateTime.now()))).doesNotThrowAnyException();
    }

    @Test
    void sinConexionPublicarNoHaceNadaNiLanza() {
        WebSocketClient cliente = mock(WebSocketClient.class);
        PublicadorUbicacion publicador = new PublicadorUbicacion(mock(JwtService.class), JsonMapper.builder().build(),
                mock(TaskScheduler.class), cliente, "ws://localhost:8083/internal/ws/eventos");

        assertThatCode(() -> publicador.publicar(7L, BigDecimal.ONE, BigDecimal.TEN, LocalDateTime.now()))
                .doesNotThrowAnyException();
        verifyNoInteractions(cliente);
    }
}
