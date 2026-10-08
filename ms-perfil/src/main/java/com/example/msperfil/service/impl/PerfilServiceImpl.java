package com.example.msperfil.service.impl;

import com.example.msperfil.dto.request.*;
import com.example.msperfil.dto.response.*;
import com.example.msperfil.entity.*;
import com.example.msperfil.repository.*;
import com.example.msperfil.service.PerfilService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilServiceImpl implements PerfilService {
    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final DespachadorRepository despachadorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final SolicitudRepository solicitudRepository;

    public PerfilServiceImpl(UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            DespachadorRepository despachadorRepository,
            VehiculoRepository vehiculoRepository,
            SolicitudRepository solicitudRepository) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.despachadorRepository = despachadorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.solicitudRepository = solicitudRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilConductorResponse obtenerPerfilConductor() {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.CONDUCTOR);
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un perfil de conductor asociado al usuario"));
        return construirPerfilConductor(usuario, conductor);
    }

    @Override
    @Transactional
    public PerfilConductorResponse guardarPerfilConductor(PerfilConductorRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.CONDUCTOR);
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> conductorRepository.save(Conductor.builder()
                        .usuario(usuario)
                        .calificacionPromedio(BigDecimal.ZERO)
                        .cancelacionesTotales(0)
                        .build()));

        if (conductor.getCalificacionPromedio() == null) {
            conductor.setCalificacionPromedio(BigDecimal.ZERO);
        }
        if (conductor.getCancelacionesTotales() == null) {
            conductor.setCancelacionesTotales(0);
        }
        if (request.getUbicacionLat() != null && request.getUbicacionLng() != null) {
            conductor.setUbicacionLat(request.getUbicacionLat());
            conductor.setUbicacionLng(request.getUbicacionLng());
        }

        Conductor conductorGuardado = conductorRepository.save(conductor);
        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(conductorGuardado.getId());
        Optional<Vehiculo> vehiculoActualActivo = vehiculos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .findFirst();
        boolean cambiaVehiculo = vehiculoActualActivo.isPresent()
                && !mismaPlaca(vehiculoActualActivo.get().getPlaca(), request.getPlaca());

        if (cambiaVehiculo && solicitudRepository.existsByConductorIdAndEstadoIn(
                conductorGuardado.getId(),
                List.of(Solicitud.EstadoSolicitud.ACEPTADA, Solicitud.EstadoSolicitud.EN_CURSO))) {
            throw new IllegalStateException(
                    "No puedes cambiar de vehículo mientras tengas una solicitud aceptada o en curso.");
        }

        vehiculos.forEach(vehiculo -> vehiculo.setActivo(false));
        Vehiculo vehiculoActivo = vehiculos.stream()
                .filter(vehiculo -> mismaPlaca(vehiculo.getPlaca(), request.getPlaca()))
                .findFirst()
                .orElseGet(() -> Vehiculo.builder()
                        .conductor(conductorGuardado)
                        .estadoVerificacion(Vehiculo.EstadoVerificacion.PENDIENTE)
                        .build());
        vehiculoActivo.setTipoVehiculo(normalizar(request.getTipoVehiculo()));
        vehiculoActivo.setPlaca(normalizar(request.getPlaca()).toUpperCase());
        vehiculoActivo.setCapacidadCarga(request.getCapacidadCarga());
        vehiculoActivo.setActivo(true);
        vehiculoRepository.saveAll(vehiculos);
        vehiculoRepository.save(vehiculoActivo);
        return construirPerfilConductor(usuario, conductorGuardado);
    }

    @Override
    @Transactional
    public PerfilConductorResponse activarVehiculo(Long vehiculoId) {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.CONDUCTOR);
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un perfil de conductor asociado al usuario"));
        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(conductor.getId());
        Vehiculo vehiculoAActivar = vehiculos.stream()
                .filter(v -> v.getId().equals(vehiculoId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "El vehículo no existe o no pertenece a este conductor"));

        if (Boolean.TRUE.equals(vehiculoAActivar.getActivo())) {
            return construirPerfilConductor(usuario, conductor);
        }
        if (solicitudRepository.existsByConductorIdAndEstadoIn(
                conductor.getId(),
                List.of(Solicitud.EstadoSolicitud.ACEPTADA, Solicitud.EstadoSolicitud.EN_CURSO))) {
            throw new IllegalStateException(
                    "No puedes cambiar de vehículo mientras tengas una solicitud aceptada o en curso.");
        }
        vehiculos.forEach(v -> v.setActivo(false));
        vehiculoAActivar.setActivo(true);
        vehiculoRepository.saveAll(vehiculos);
        return construirPerfilConductor(usuario, conductor);
    }

    @Override
    @Transactional
    public void actualizarUbicacionConductor(UbicacionConductorRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.CONDUCTOR);
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un perfil de conductor asociado al usuario"));
        if (!solicitudRepository.existsByConductorIdAndEstadoIn(
                conductor.getId(),
                List.of(Solicitud.EstadoSolicitud.ACEPTADA, Solicitud.EstadoSolicitud.EN_CURSO))) {
            throw new IllegalStateException(
                    "Solo se comparte la ubicación mientras tienes un viaje activo.");
        }
        conductor.setUbicacionLat(request.getLatitud());
        conductor.setUbicacionLng(request.getLongitud());
        conductor.setUbicacionActualizadaEn(LocalDateTime.now());
        conductorRepository.save(conductor);
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilDespachadorResponse obtenerPerfilDespachador() {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.DESPACHADOR);
        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un perfil de despachador asociado al usuario"));
        return construirPerfilDespachador(usuario, despachador);
    }

    @Override
    @Transactional
    public PerfilDespachadorResponse guardarPerfilDespachador(PerfilDespachadorRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.DESPACHADOR);
        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> despachadorRepository.save(Despachador.builder()
                        .usuario(usuario)
                        .build()));
        despachador.setNombreEmpresa(normalizarOpcional(request.getNombreEmpresa()));
        despachador.setNit(normalizarOpcional(request.getNit()));
        Despachador despachadorGuardado = despachadorRepository.save(despachador);
        return construirPerfilDespachador(usuario, despachadorGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public DatosPersonalesResponse obtenerDatosPersonales() {
        return construirDatosPersonales(obtenerUsuarioAutenticado());
    }

    @Override
    @Transactional
    public DatosPersonalesResponse actualizarDatosPersonales(DatosPersonalesRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        usuario.setNombre(request.getNombre().trim());
        usuario.setTelefono(request.getTelefono().trim());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return construirDatosPersonales(usuarioGuardado);
    }

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
    }

    private void validarRol(Usuario usuario, Usuario.TipoUsuario tipoEsperado) {
        if (usuario.getTipoUsuario() != tipoEsperado) {
            throw new IllegalStateException(
                    "El perfil solicitado no corresponde al rol del usuario autenticado");
        }
    }

    private PerfilConductorResponse construirPerfilConductor(Usuario usuario, Conductor conductor) {
        List<VehiculoPerfilResponse> vehiculos = vehiculoRepository.findByConductorId(conductor.getId())
                .stream()
                .map(this::construirVehiculoResponse)
                .toList();
        return new PerfilConductorResponse(
                conductor.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getTelefono(),
                conductor.getCalificacionPromedio() == null ? BigDecimal.ZERO : conductor.getCalificacionPromedio(),
                conductor.getCancelacionesTotales() == null ? 0 : conductor.getCancelacionesTotales(),
                conductor.getUbicacionLat(), conductor.getUbicacionLng(), vehiculos);
    }

    private PerfilDespachadorResponse construirPerfilDespachador(Usuario usuario, Despachador despachador) {
        return new PerfilDespachadorResponse(
                despachador.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getTelefono(),
                despachador.getNombreEmpresa(), despachador.getNit(),
                solicitudRepository.countByDespachadorId(despachador.getId()));
    }

    private DatosPersonalesResponse construirDatosPersonales(Usuario usuario) {
        return new DatosPersonalesResponse(
                usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getTelefono(), usuario.getTipoUsuario());
    }

    private VehiculoPerfilResponse construirVehiculoResponse(Vehiculo vehiculo) {
        return new VehiculoPerfilResponse(
                vehiculo.getId(), vehiculo.getTipoVehiculo(), vehiculo.getPlaca(),
                vehiculo.getCapacidadCarga(), vehiculo.getEstadoVerificacion().name(), vehiculo.getActivo());
    }

    private boolean mismaPlaca(String placaActual, String placaNueva) {
        return placaActual != null && placaNueva != null
                && placaActual.equalsIgnoreCase(placaNueva.trim());
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null || normalizado.isBlank() ? null : normalizado;
    }
}
