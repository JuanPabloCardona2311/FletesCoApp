package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.request.AceptarSolicitudRequest;
import org.springframework.security.access.AccessDeniedException;
import com.example.demo.dto.request.PublicarSolicitudRequest;
import com.example.demo.dto.response.SolicitudAceptadaResponse;
import com.example.demo.dto.response.SolicitudPublicadaResponse;
import com.example.demo.entity.Conductor;
import com.example.demo.entity.Despachador;
import com.example.demo.entity.HistorialEstadoSolicitud;
import com.example.demo.entity.Pago;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.Vehiculo;
import com.example.demo.repository.ConductorRepository;
import com.example.demo.repository.DespachadorRepository;
import com.example.demo.repository.HistorialEstadoSolicitudRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SolicitudRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.VehiculoRepository;
import com.example.demo.service.SolicitudService;
import com.example.demo.dto.response.SolicitudDetalleResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final DespachadorRepository despachadorRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PagoRepository pagoRepository;
    private final HistorialEstadoSolicitudRepository historialEstadoSolicitudRepository;

    public SolicitudServiceImpl(
            SolicitudRepository solicitudRepository,
            UsuarioRepository usuarioRepository,
            DespachadorRepository despachadorRepository,
            ConductorRepository conductorRepository,
            VehiculoRepository vehiculoRepository,
            PagoRepository pagoRepository,
            HistorialEstadoSolicitudRepository historialEstadoSolicitudRepository
    ) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.despachadorRepository = despachadorRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.pagoRepository = pagoRepository;
        this.historialEstadoSolicitudRepository = historialEstadoSolicitudRepository;
    }

    @Override
    @Transactional
    public SolicitudPublicadaResponse publicarSolicitud(PublicarSolicitudRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.DESPACHADOR) {
            throw new IllegalStateException("Solo un despachador puede publicar solicitudes");
        }

        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe un despachador asociado a este usuario"));

        Solicitud solicitud = Solicitud.builder()
                .despachador(despachador)
                .origen(request.getOrigen())
                .destino(request.getDestino())
                .origenLat(request.getOrigenLat())
                .origenLng(request.getOrigenLng())
                .destinoLat(request.getDestinoLat())
                .destinoLng(request.getDestinoLng())
                .tipoCarga(request.getTipoCarga())
                .tipoVehiculoRequerido(request.getTipoVehiculoRequerido())
                .peso(request.getPeso())
                .precioOfrecido(request.getPrecioOfrecido())
                .fechaPublicacion(LocalDateTime.now())
                .fechaRecogida(request.getFechaRecogida())
                .fechaEntregaEstimada(request.getFechaEntregaEstimada())
                .requiereCitaPuerto(request.getRequiereCitaPuerto())
                .numeroCita(request.getNumeroCita())
                .estado(Solicitud.EstadoSolicitud.PUBLICADA)
                .build();

        Solicitud guardada = solicitudRepository.save(solicitud);

        HistorialEstadoSolicitud historial = HistorialEstadoSolicitud.builder()
                .solicitud(guardada)
                .estadoAnterior("NINGUNO")
                .estadoNuevo(Solicitud.EstadoSolicitud.PUBLICADA.name())
                .fechaCambio(LocalDateTime.now())
                .build();
        historialEstadoSolicitudRepository.save(historial);

        return new SolicitudPublicadaResponse(
                guardada.getId(),
                guardada.getOrigen(),
                guardada.getDestino(),
                guardada.getTipoCarga(),
                guardada.getTipoVehiculoRequerido(),
                guardada.getPrecioOfrecido(),
                guardada.getEstado().name(),
                guardada.getFechaPublicacion(),
                guardada.getDespachador().getId()
        );
    }

    @Override
    public List<SolicitudPublicadaResponse> listarSolicitudesDisponibles() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.CONDUCTOR) {
            throw new IllegalStateException("Solo un conductor puede consultar solicitudes disponibles");
        }

        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe un conductor asociado a este usuario"));

        List<Vehiculo> vehiculos = vehiculoRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId()))
                .collect(Collectors.toList());

        if (vehiculos.isEmpty()) {
            return List.of();
        }

        return solicitudRepository.findAll().stream()
                .filter(s -> s.getEstado() == Solicitud.EstadoSolicitud.PUBLICADA)
                .filter(s -> vehiculos.stream().anyMatch(v ->
                        v.getCapacidadCarga() != null &&
                                v.getCapacidadCarga().compareTo(s.getPeso()) >= 0 &&
                                esVehiculoCompatible(v.getTipoVehiculo(), s.getTipoVehiculoRequerido())
                ))
                .map(s -> new SolicitudPublicadaResponse(
                        s.getId(),
                        s.getOrigen(),
                        s.getDestino(),
                        s.getTipoCarga(),
                        s.getTipoVehiculoRequerido(),
                        s.getPrecioOfrecido(),
                        s.getEstado().name(),
                        s.getFechaPublicacion(),
                        s.getDespachador().getId()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SolicitudAceptadaResponse aceptarSolicitud(AceptarSolicitudRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.CONDUCTOR) {
            throw new IllegalStateException("Solo un conductor puede aceptar solicitudes");
        }

        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe un conductor asociado a este usuario"));

        Solicitud solicitud = solicitudRepository.findById(request.getSolicitudId())
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.PUBLICADA) {
            throw new IllegalStateException("La solicitud no está disponible para aceptar");
        }

        boolean tieneVehiculoValido = vehiculoRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId()))
                .anyMatch(v ->
                        v.getCapacidadCarga() != null &&
                                v.getCapacidadCarga().compareTo(solicitud.getPeso()) >= 0 &&
                                esVehiculoCompatible(v.getTipoVehiculo(), solicitud.getTipoVehiculoRequerido())
                );

        if (!tieneVehiculoValido) {
            throw new IllegalStateException("El conductor no cuenta con un vehículo compatible para aceptar esta solicitud");
        }

        solicitud.setConductor(conductor);
        solicitud.setEstado(Solicitud.EstadoSolicitud.ACEPTADA);
        Solicitud solicitudAceptada = solicitudRepository.save(solicitud);

        HistorialEstadoSolicitud historial = HistorialEstadoSolicitud.builder()
                .solicitud(solicitudAceptada)
                .estadoAnterior(Solicitud.EstadoSolicitud.PUBLICADA.name())
                .estadoNuevo(Solicitud.EstadoSolicitud.ACEPTADA.name())
                .fechaCambio(LocalDateTime.now())
                .build();
        historialEstadoSolicitudRepository.save(historial);

        BigDecimal comision = solicitudAceptada.getPrecioOfrecido().multiply(new BigDecimal("0.10"));
        BigDecimal monto = solicitudAceptada.getPrecioOfrecido();
        BigDecimal montoNetoConductor = monto.subtract(comision);

        Pago pago = Pago.builder()
                .solicitud(solicitudAceptada)
                .monto(monto)
                .comisionPlataforma(comision)
                .montoNetoConductor(montoNetoConductor)
                .estado(Pago.EstadoPago.RETENIDO)
                .fechaLimiteConfirmacion(solicitudAceptada.getFechaEntregaEstimada().plusDays(3))
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        return new SolicitudAceptadaResponse(
                pagoGuardado.getId(),
                solicitudAceptada.getId(),
                conductor.getId(),
                solicitudAceptada.getDespachador().getUsuario().getTelefono(),
                solicitudAceptada.getEstado().name(),
                LocalDateTime.now(),
                "Solicitud aceptada correctamente"
        );
    }

    @Override
public SolicitudDetalleResponse obtenerSolicitudPorId(Long id) {

    String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() ->
                    new EntityNotFoundException(
                            "Usuario autenticado no encontrado"
                    )
            );

    Despachador despachador =
            despachadorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "No existe un despachador asociado a este usuario"
                            )
                    );

    Solicitud solicitud = solicitudRepository.findById(id)
            .orElseThrow(() ->
                    new EntityNotFoundException(
                            "Solicitud no encontrada"
                    )
            );

    if (!solicitud.getDespachador()
            .getId()
            .equals(despachador.getId())) {

        throw new AccessDeniedException(
                "No tienes permiso para consultar esta solicitud"
        );
    }

    return new SolicitudDetalleResponse(
            solicitud.getId(),

            solicitud.getOrigen(),
            solicitud.getDestino(),

            solicitud.getOrigenLat(),
            solicitud.getOrigenLng(),

            solicitud.getDestinoLat(),
            solicitud.getDestinoLng(),

            solicitud.getTipoCarga(),
            solicitud.getTipoVehiculoRequerido(),

            solicitud.getPeso(),
            solicitud.getPrecioOfrecido(),

            solicitud.getFechaPublicacion(),
            solicitud.getFechaRecogida(),
            solicitud.getFechaEntregaEstimada(),

            solicitud.getRequiereCitaPuerto(),
            solicitud.getNumeroCita(),

            solicitud.getEstado().name()
    );
}

    private boolean esVehiculoCompatible(String tipoVehiculo, String tipoRequerido) {
        if (tipoVehiculo == null || tipoRequerido == null) {
            return false;
        }

        String vehiculoNormalizado = tipoVehiculo.trim().toLowerCase();
        String requeridoNormalizado = tipoRequerido.trim().toLowerCase();

        return vehiculoNormalizado.equals(requeridoNormalizado)
                || requeridoNormalizado.contains(vehiculoNormalizado)
                || vehiculoNormalizado.contains(requeridoNormalizado);
    }
}
