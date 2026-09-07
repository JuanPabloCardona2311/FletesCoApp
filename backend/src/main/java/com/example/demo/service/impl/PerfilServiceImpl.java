package com.example.demo.service.impl;

import com.example.demo.dto.request.PerfilConductorRequest;
import com.example.demo.dto.request.PerfilDespachadorRequest;
import com.example.demo.dto.response.PerfilConductorResponse;
import com.example.demo.dto.response.PerfilDespachadorResponse;
import com.example.demo.dto.response.VehiculoPerfilResponse;
import com.example.demo.entity.Conductor;
import com.example.demo.entity.Despachador;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.Vehiculo;
import com.example.demo.repository.ConductorRepository;
import com.example.demo.repository.DespachadorRepository;
import com.example.demo.repository.SolicitudRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.VehiculoRepository;
import com.example.demo.service.PerfilService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PerfilServiceImpl implements PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final DespachadorRepository despachadorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final SolicitudRepository solicitudRepository;

    public PerfilServiceImpl(
            UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            DespachadorRepository despachadorRepository,
            VehiculoRepository vehiculoRepository,
            SolicitudRepository solicitudRepository
    ) {
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
                .orElseThrow(() -> new EntityNotFoundException("No existe un perfil de conductor asociado al usuario"));

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

        conductor.setUbicacionLat(request.getUbicacionLat());
        conductor.setUbicacionLng(request.getUbicacionLng());
        Conductor conductorGuardado = conductorRepository.save(conductor);

        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(conductorGuardado.getId());
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
    @Transactional(readOnly = true)
    public PerfilDespachadorResponse obtenerPerfilDespachador() {
        Usuario usuario = obtenerUsuarioAutenticado();
        validarRol(usuario, Usuario.TipoUsuario.DESPACHADOR);

        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe un perfil de despachador asociado al usuario"));

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

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
    }

    private void validarRol(Usuario usuario, Usuario.TipoUsuario tipoEsperado) {
        if (usuario.getTipoUsuario() != tipoEsperado) {
            throw new IllegalStateException("El perfil solicitado no corresponde al rol del usuario autenticado");
        }
    }

    private PerfilConductorResponse construirPerfilConductor(Usuario usuario, Conductor conductor) {
        List<VehiculoPerfilResponse> vehiculos = vehiculoRepository.findByConductorId(conductor.getId())
                .stream()
                .map(this::construirVehiculoResponse)
                .toList();

        return new PerfilConductorResponse(
                conductor.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                conductor.getCalificacionPromedio() == null ? BigDecimal.ZERO : conductor.getCalificacionPromedio(),
                conductor.getCancelacionesTotales() == null ? 0 : conductor.getCancelacionesTotales(),
                conductor.getUbicacionLat(),
                conductor.getUbicacionLng(),
                vehiculos
        );
    }

    private PerfilDespachadorResponse construirPerfilDespachador(Usuario usuario, Despachador despachador) {
        return new PerfilDespachadorResponse(
                despachador.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                despachador.getNombreEmpresa(),
                despachador.getNit(),
                solicitudRepository.countByDespachadorId(despachador.getId())
        );
    }

    private VehiculoPerfilResponse construirVehiculoResponse(Vehiculo vehiculo) {
        return new VehiculoPerfilResponse(
                vehiculo.getId(),
                vehiculo.getTipoVehiculo(),
                vehiculo.getPlaca(),
                vehiculo.getCapacidadCarga(),
                vehiculo.getEstadoVerificacion().name(),
                vehiculo.getActivo()
        );
    }

    private boolean mismaPlaca(String placaActual, String placaNueva) {
        return placaActual != null && placaNueva != null && placaActual.equalsIgnoreCase(placaNueva.trim());
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null || normalizado.isBlank() ? null : normalizado;
    }
}
