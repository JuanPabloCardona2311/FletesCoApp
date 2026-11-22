package com.example.mssolicitudes.service;

import com.example.mssolicitudes.dto.request.AceptarSolicitudRequest;
import com.example.mssolicitudes.dto.request.PublicarSolicitudRequest;
import com.example.mssolicitudes.entity.Conductor;
import com.example.mssolicitudes.entity.Despachador;
import com.example.mssolicitudes.entity.Pago;
import com.example.mssolicitudes.entity.Solicitud;
import com.example.mssolicitudes.entity.Usuario;
import com.example.mssolicitudes.entity.Vehiculo;
import com.example.mssolicitudes.evento.EventoSolicitud;
import com.example.mssolicitudes.repository.ConductorRepository;
import com.example.mssolicitudes.repository.DespachadorRepository;
import com.example.mssolicitudes.repository.HistorialEstadoSolicitudRepository;
import com.example.mssolicitudes.repository.PagoRepository;
import com.example.mssolicitudes.repository.SolicitudRepository;
import com.example.mssolicitudes.repository.UsuarioRepository;
import com.example.mssolicitudes.repository.VehiculoRepository;
import com.example.mssolicitudes.service.impl.SolicitudServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cada transición de la solicitud publica su EventoSolicitud con el tipo y los estados correctos.
class SolicitudServiceEventosTest {

    private static final String CORREO_DESPACHADOR = "despachador@ejemplo.test";
    private static final String CORREO_CONDUCTOR = "conductor@ejemplo.test";

    private SolicitudRepository solicitudRepository;
    private UsuarioRepository usuarioRepository;
    private DespachadorRepository despachadorRepository;
    private ConductorRepository conductorRepository;
    private VehiculoRepository vehiculoRepository;
    private PagoRepository pagoRepository;
    private ApplicationEventPublisher eventPublisher;
    private SolicitudServiceImpl servicio;

    private Despachador despachador;
    private Conductor conductor;

    @BeforeEach
    void preparar() {
        solicitudRepository = mock(SolicitudRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        despachadorRepository = mock(DespachadorRepository.class);
        conductorRepository = mock(ConductorRepository.class);
        vehiculoRepository = mock(VehiculoRepository.class);
        pagoRepository = mock(PagoRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        servicio = new SolicitudServiceImpl(solicitudRepository, usuarioRepository, despachadorRepository,
                conductorRepository, vehiculoRepository, pagoRepository,
                mock(HistorialEstadoSolicitudRepository.class), eventPublisher);

        Usuario usuarioDespachador = Usuario.builder().id(1L).email(CORREO_DESPACHADOR).nombre("Despachador")
                .telefono("3000000000").tipoUsuario(Usuario.TipoUsuario.DESPACHADOR).build();
        Usuario usuarioConductor = Usuario.builder().id(2L).email(CORREO_CONDUCTOR).nombre("Conductor")
                .tipoUsuario(Usuario.TipoUsuario.CONDUCTOR).build();
        despachador = Despachador.builder().id(10L).usuario(usuarioDespachador).build();
        conductor = Conductor.builder().id(20L).usuario(usuarioConductor).cancelacionesTotales(0).build();

        when(usuarioRepository.findByEmail(CORREO_DESPACHADOR)).thenReturn(Optional.of(usuarioDespachador));
        when(usuarioRepository.findByEmail(CORREO_CONDUCTOR)).thenReturn(Optional.of(usuarioConductor));
        when(despachadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(despachador));
        when(conductorRepository.findByUsuarioId(2L)).thenReturn(Optional.of(conductor));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(invocacion -> {
            Solicitud s = invocacion.getArgument(0);
            if (s.getId() == null) {
                s.setId(100L);
            }
            return s;
        });
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicarSolicitudPublicaSolicitudCreada() {
        autenticar(CORREO_DESPACHADOR);
        PublicarSolicitudRequest request = new PublicarSolicitudRequest("Medellín", "Bogotá",
                new BigDecimal("6.2"), new BigDecimal("-75.5"), new BigDecimal("4.7"), new BigDecimal("-74.0"),
                "Cajas", "Camión", new BigDecimal("2"), new BigDecimal("1000000"),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), false, null);

        servicio.publicarSolicitud(request);

        verificarEvento(EventoSolicitud.SOLICITUD_CREADA, null, "PUBLICADA", null);
    }

    @Test
    void aceptarSolicitudPublicaSolicitudAceptada() {
        autenticar(CORREO_CONDUCTOR);
        when(solicitudRepository.existsByConductorIdAndEstadoIn(anyLong(), anyCollection())).thenReturn(false);
        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud(Solicitud.EstadoSolicitud.PUBLICADA, null)));
        when(vehiculoRepository.findAll()).thenReturn(List.of(Vehiculo.builder().id(5L).conductor(conductor)
                .tipoVehiculo("Camión").capacidadCarga(new BigDecimal("8")).activo(true).build()));

        servicio.aceptarSolicitud(new AceptarSolicitudRequest(100L, new BigDecimal("6.2"), new BigDecimal("-75.5")));

        verificarEvento(EventoSolicitud.SOLICITUD_ACEPTADA, "PUBLICADA", "ACEPTADA", 20L);
    }

    @Test
    void iniciarViajePublicaViajeIniciado() {
        autenticar(CORREO_CONDUCTOR);
        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud(Solicitud.EstadoSolicitud.ACEPTADA, conductor)));

        servicio.iniciarViaje(100L);

        verificarEvento(EventoSolicitud.VIAJE_INICIADO, "ACEPTADA", "EN_CURSO", 20L);
    }

    @Test
    void marcarEntregadaPublicaEntregaMarcada() {
        autenticar(CORREO_CONDUCTOR);
        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud(Solicitud.EstadoSolicitud.EN_CURSO, conductor)));

        servicio.marcarEntregada(100L);

        verificarEvento(EventoSolicitud.ENTREGA_MARCADA, "EN_CURSO", "COMPLETADA", 20L);
    }

    @Test
    void confirmarEntregaPublicaPagoLiberado() {
        autenticar(CORREO_DESPACHADOR);
        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud(Solicitud.EstadoSolicitud.COMPLETADA, conductor)));
        when(pagoRepository.findBySolicitudId(100L)).thenReturn(Optional.of(Pago.builder().id(7L)
                .monto(new BigDecimal("1000000")).estado(Pago.EstadoPago.RETENIDO).build()));

        servicio.confirmarEntrega(100L);

        verificarEvento(EventoSolicitud.PAGO_LIBERADO, "COMPLETADA", "COMPLETADA", 20L);
    }

    private void verificarEvento(String tipo, String estadoAnterior, String estadoNuevo, Long conductorId) {
        ArgumentCaptor<EventoSolicitud> captor = ArgumentCaptor.forClass(EventoSolicitud.class);
        verify(eventPublisher).publishEvent(captor.capture());
        EventoSolicitud evento = captor.getValue();
        assertThat(evento.tipo()).isEqualTo(tipo);
        assertThat(evento.solicitudId()).isEqualTo(100L);
        assertThat(evento.estadoAnterior()).isEqualTo(estadoAnterior);
        assertThat(evento.estadoNuevo()).isEqualTo(estadoNuevo);
        assertThat(evento.despachadorId()).isEqualTo(10L);
        assertThat(evento.conductorId()).isEqualTo(conductorId);
        assertThat(evento.fecha()).isNotBlank();
    }

    private Solicitud solicitud(Solicitud.EstadoSolicitud estado, Conductor asignado) {
        return Solicitud.builder().id(100L).despachador(despachador).conductor(asignado)
                .origen("Medellín").destino("Bogotá").tipoCarga("Cajas").tipoVehiculoRequerido("Camión")
                .peso(new BigDecimal("2")).precioOfrecido(new BigDecimal("1000000"))
                .fechaPublicacion(LocalDateTime.now()).fechaRecogida(LocalDateTime.now().plusDays(1))
                .fechaEntregaEstimada(LocalDateTime.now().plusDays(2)).requiereCitaPuerto(false)
                .estado(estado).build();
    }

    private static void autenticar(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, List.of()));
    }
}
