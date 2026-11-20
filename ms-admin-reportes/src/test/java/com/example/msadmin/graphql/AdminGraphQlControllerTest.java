package com.example.msadmin.graphql;

import com.example.msadmin.entity.Conductor;
import com.example.msadmin.entity.Solicitud;
import com.example.msadmin.entity.Usuario;
import com.example.msadmin.entity.Vehiculo;
import com.example.msadmin.repository.ConductorRepository;
import com.example.msadmin.repository.DespachadorRepository;
import com.example.msadmin.repository.SolicitudRepository;
import com.example.msadmin.repository.UsuarioRepository;
import com.example.msadmin.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Slice de GraphQL: carga el esquema y el controlador, con los repositorios simulados.
// La regla de rol (@PreAuthorize) no se carga aquí; se prueba a mano con curl.
@GraphQlTest(AdminGraphQlController.class)
class AdminGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private ConductorRepository conductorRepository;
    @MockitoBean
    private DespachadorRepository despachadorRepository;
    @MockitoBean
    private VehiculoRepository vehiculoRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void solicitudDevuelveConductorConUsuarioYVehiculos() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud(10L, 7L)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(7L, 70L)));
        when(usuarioRepository.findById(70L)).thenReturn(Optional.of(usuario(70L, "Carlos Pérez")));
        when(vehiculoRepository.findByConductorIdIn(anyCollection()))
                .thenReturn(List.of(vehiculo(1L, 7L, "TKX482"), vehiculo(2L, 7L, "GHT219")));

        graphQlTester.document("""
                        { solicitud(id: 10) { origen conductor { usuario { nombre estado } vehiculos { placa } } } }
                        """)
                .execute()
                .path("solicitud.origen").entity(String.class).isEqualTo("Medellín")
                .path("solicitud.conductor.usuario.nombre").entity(String.class).isEqualTo("Carlos Pérez")
                .path("solicitud.conductor.usuario.estado").entity(String.class).isEqualTo("ACTIVO")
                .path("solicitud.conductor.vehiculos[*].placa").entityList(String.class)
                .containsExactly("TKX482", "GHT219");
    }

    @Test
    void solicitudSinConductorDevuelveConductorNull() {
        when(solicitudRepository.findById(11L)).thenReturn(Optional.of(solicitud(11L, null)));

        graphQlTester.document("{ solicitud(id: 11) { id conductor { id } } }")
                .execute()
                .path("solicitud.id").entity(String.class).isEqualTo("11")
                .path("solicitud.conductor").valueIsNull();

        verify(conductorRepository, times(0)).findById(any());
    }

    @Test
    void solicitudesAjustaElLimiteAlMaximo() {
        when(solicitudRepository.buscarRecientes(isNull(), any(Pageable.class))).thenReturn(List.of());

        graphQlTester.document("{ solicitudes(limite: 500) { id } }")
                .execute()
                .path("solicitudes").entityList(Object.class).hasSize(0);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudRepository).buscarRecientes(isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(AdminGraphQlController.LIMITE_MAXIMO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void vehiculosDeVariosConductoresUsanUnaSolaConsulta() {
        when(solicitudRepository.buscarRecientes(any(), any(Pageable.class)))
                .thenReturn(List.of(solicitud(1L, 7L), solicitud(2L, 8L)));
        when(conductorRepository.findById(7L)).thenReturn(Optional.of(conductor(7L, 70L)));
        when(conductorRepository.findById(8L)).thenReturn(Optional.of(conductor(8L, 80L)));
        when(vehiculoRepository.findByConductorIdIn(anyCollection()))
                .thenReturn(List.of(vehiculo(1L, 7L, "TKX482")));

        graphQlTester.document("{ solicitudes(estado: ACEPTADA) { conductor { id vehiculos { placa } } } }")
                .execute()
                .path("solicitudes[0].conductor.vehiculos[*].placa").entityList(String.class).containsExactly("TKX482")
                .path("solicitudes[1].conductor.vehiculos").entityList(Object.class).hasSize(0);

        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(vehiculoRepository, times(1)).findByConductorIdIn(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(7L, 8L);
    }

    // Las entidades son de solo lectura (sin setters): los datos de prueba se cargan por reflexión.
    private static Solicitud solicitud(Long id, Long conductorId) {
        Solicitud s = new Solicitud();
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "origen", "Medellín");
        ReflectionTestUtils.setField(s, "destino", "Bogotá");
        ReflectionTestUtils.setField(s, "despachadorId", 3L);
        ReflectionTestUtils.setField(s, "conductorId", conductorId);
        ReflectionTestUtils.setField(s, "precioOfrecido", new BigDecimal("1850000"));
        ReflectionTestUtils.setField(s, "estado", Solicitud.EstadoSolicitud.ACEPTADA);
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 1, 8, 0);
        ReflectionTestUtils.setField(s, "fechaPublicacion", fecha);
        ReflectionTestUtils.setField(s, "fechaRecogida", fecha.plusDays(1));
        ReflectionTestUtils.setField(s, "fechaEntregaEstimada", fecha.plusDays(2));
        return s;
    }

    private static Conductor conductor(Long id, Long usuarioId) {
        Conductor c = new Conductor();
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "usuarioId", usuarioId);
        ReflectionTestUtils.setField(c, "calificacionPromedio", new BigDecimal("4.70"));
        ReflectionTestUtils.setField(c, "cancelacionesTotales", 0);
        return c;
    }

    private static Vehiculo vehiculo(Long id, Long conductorId, String placa) {
        Vehiculo v = new Vehiculo();
        ReflectionTestUtils.setField(v, "id", id);
        ReflectionTestUtils.setField(v, "conductorId", conductorId);
        ReflectionTestUtils.setField(v, "tipoVehiculo", "CAMION");
        ReflectionTestUtils.setField(v, "placa", placa);
        ReflectionTestUtils.setField(v, "capacidadCarga", new BigDecimal("8"));
        ReflectionTestUtils.setField(v, "estadoVerificacion", "VERIFICADO");
        ReflectionTestUtils.setField(v, "activo", true);
        return v;
    }

    private static Usuario usuario(Long id, String nombre) {
        return Usuario.builder().id(id).nombre(nombre).email("conductor@ejemplo.test")
                .estado(Usuario.EstadoUsuario.ACTIVO).build();
    }
}
