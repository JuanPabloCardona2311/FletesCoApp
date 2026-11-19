package com.example.msadmin.graphql;

import com.example.msadmin.entity.Conductor;
import com.example.msadmin.entity.Despachador;
import com.example.msadmin.entity.Solicitud;
import com.example.msadmin.entity.Usuario;
import com.example.msadmin.entity.Vehiculo;
import com.example.msadmin.repository.ConductorRepository;
import com.example.msadmin.repository.DespachadorRepository;
import com.example.msadmin.repository.SolicitudRepository;
import com.example.msadmin.repository.UsuarioRepository;
import com.example.msadmin.repository.VehiculoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// API GraphQL de solo lectura para el panel del administrador (/api/admin/graphql).
// Cada relación (despachador, conductor, usuario, vehículos) se consulta solo si el
// cliente la pide en la query.
@Controller
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminGraphQlController {

    static final int LIMITE_POR_DEFECTO = 20;
    static final int LIMITE_MAXIMO = 50;

    private final SolicitudRepository solicitudRepository;
    private final ConductorRepository conductorRepository;
    private final DespachadorRepository despachadorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminGraphQlController(
            SolicitudRepository solicitudRepository,
            ConductorRepository conductorRepository,
            DespachadorRepository despachadorRepository,
            VehiculoRepository vehiculoRepository,
            UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.conductorRepository = conductorRepository;
        this.despachadorRepository = despachadorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Una solicitud por id; null si no existe.
    @QueryMapping
    public Solicitud solicitud(@Argument Long id) {
        return solicitudRepository.findById(id).orElse(null);
    }

    // Últimas solicitudes, con estado opcional. El límite se ajusta a 1..50.
    @QueryMapping
    public List<Solicitud> solicitudes(@Argument Solicitud.EstadoSolicitud estado, @Argument Integer limite) {
        int tamano = Math.max(1, Math.min(LIMITE_MAXIMO, limite == null ? LIMITE_POR_DEFECTO : limite));
        return solicitudRepository.buscarRecientes(estado, PageRequest.of(0, tamano));
    }

    // Un conductor por id; null si no existe.
    @QueryMapping
    public Conductor conductor(@Argument Long id) {
        return conductorRepository.findById(id).orElse(null);
    }

    // Despachador que publicó la solicitud.
    @SchemaMapping(typeName = "Solicitud")
    public Despachador despachador(Solicitud solicitud) {
        return despachadorRepository.findById(solicitud.getDespachadorId()).orElse(null);
    }

    // Conductor asignado; null mientras la solicitud no haya sido aceptada.
    @SchemaMapping(typeName = "Solicitud")
    public Conductor conductor(Solicitud solicitud) {
        if (solicitud.getConductorId() == null) {
            return null;
        }
        return conductorRepository.findById(solicitud.getConductorId()).orElse(null);
    }

    // Usuario (nombre, correo, teléfono) del despachador.
    @SchemaMapping(typeName = "Despachador")
    public Usuario usuario(Despachador despachador) {
        return buscarUsuario(despachador.getUsuarioId());
    }

    // Usuario (nombre, correo, teléfono) del conductor.
    @SchemaMapping(typeName = "Conductor")
    public Usuario usuario(Conductor conductor) {
        return buscarUsuario(conductor.getUsuarioId());
    }

    // Vehículos de todos los conductores de la respuesta en una sola consulta;
    // un conductor sin vehículos recibe una lista vacía.
    @BatchMapping(typeName = "Conductor")
    public Map<Conductor, List<Vehiculo>> vehiculos(List<Conductor> conductores) {
        Map<Long, List<Vehiculo>> porConductor = vehiculoRepository
                .findByConductorIdIn(conductores.stream().map(Conductor::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(Vehiculo::getConductorId));
        return conductores.stream().distinct().collect(Collectors.toMap(
                Function.identity(),
                conductor -> porConductor.getOrDefault(conductor.getId(), List.of())));
    }

    // El estado es un enum en Java y String en el esquema.
    @SchemaMapping(typeName = "Usuario")
    public String estado(Usuario usuario) {
        return usuario.getEstado() == null ? null : usuario.getEstado().name();
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
    }
}
