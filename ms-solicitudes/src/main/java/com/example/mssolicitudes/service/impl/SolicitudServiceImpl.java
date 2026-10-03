package com.example.mssolicitudes.service.impl;

import com.example.mssolicitudes.dto.request.*;
import com.example.mssolicitudes.dto.response.*;
import com.example.mssolicitudes.entity.*;
import com.example.mssolicitudes.repository.*;
import com.example.mssolicitudes.service.SolicitudService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudServiceImpl implements SolicitudService {
    private static final List<Solicitud.EstadoSolicitud> ESTADOS_VIAJE_ACTIVO = List.of(Solicitud.EstadoSolicitud.ACEPTADA, Solicitud.EstadoSolicitud.EN_CURSO);
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final DespachadorRepository despachadorRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PagoRepository pagoRepository;
    private final HistorialEstadoSolicitudRepository historialEstadoSolicitudRepository;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository,
            DespachadorRepository despachadorRepository, ConductorRepository conductorRepository,
            VehiculoRepository vehiculoRepository, PagoRepository pagoRepository,
            HistorialEstadoSolicitudRepository historialEstadoSolicitudRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.despachadorRepository = despachadorRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.pagoRepository = pagoRepository;
        this.historialEstadoSolicitudRepository = historialEstadoSolicitudRepository;
    }

    @Override @Transactional
    public SolicitudPublicadaResponse publicarSolicitud(PublicarSolicitudRequest request) {
        Usuario usuario = usuarioActual();
        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.DESPACHADOR) throw new IllegalStateException("Solo un despachador puede publicar solicitudes");
        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId()).orElseThrow(() -> new EntityNotFoundException("No existe un despachador asociado a este usuario"));
        if (!request.getFechaEntregaEstimada().isAfter(request.getFechaRecogida())) throw new IllegalStateException("La fecha de entrega debe ser posterior a la fecha de recogida");
        Solicitud solicitud = Solicitud.builder().despachador(despachador).origen(request.getOrigen()).destino(request.getDestino())
                .origenLat(request.getOrigenLat()).origenLng(request.getOrigenLng()).destinoLat(request.getDestinoLat()).destinoLng(request.getDestinoLng())
                .tipoCarga(request.getTipoCarga()).tipoVehiculoRequerido(request.getTipoVehiculoRequerido()).peso(request.getPeso()).precioOfrecido(request.getPrecioOfrecido())
                .fechaPublicacion(LocalDateTime.now()).fechaRecogida(request.getFechaRecogida()).fechaEntregaEstimada(request.getFechaEntregaEstimada())
                .requiereCitaPuerto(request.getRequiereCitaPuerto()).numeroCita(request.getNumeroCita()).estado(Solicitud.EstadoSolicitud.PUBLICADA).build();
        Solicitud guardada = solicitudRepository.save(solicitud);
        registrarHistorial(guardada, "NINGUNO", Solicitud.EstadoSolicitud.PUBLICADA.name());
        return new SolicitudPublicadaResponse(guardada.getId(), guardada.getOrigen(), guardada.getDestino(), guardada.getTipoCarga(), guardada.getTipoVehiculoRequerido(), guardada.getPeso(), guardada.getPrecioOfrecido(), guardada.getFechaRecogida(), guardada.getFechaEntregaEstimada(), guardada.getRequiereCitaPuerto(), guardada.getEstado().name(), guardada.getFechaPublicacion(), guardada.getDespachador().getId());
    }

    @Override
    public List<SolicitudPublicadaResponse> listarSolicitudesDisponibles() {
        Usuario usuario = usuarioActual();
        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.CONDUCTOR) throw new IllegalStateException("Solo un conductor puede consultar solicitudes disponibles");
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId()).orElseThrow(() -> new EntityNotFoundException("No existe un conductor asociado a este usuario"));
        List<Vehiculo> vehiculos = vehiculoRepository.findAll().stream().filter(v -> Boolean.TRUE.equals(v.getActivo())).filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId())).collect(Collectors.toList());
        if (vehiculos.isEmpty()) return List.of();
        return solicitudRepository.findAll().stream().filter(s -> s.getEstado() == Solicitud.EstadoSolicitud.PUBLICADA)
                .filter(s -> vehiculos.stream().anyMatch(v -> v.getCapacidadCarga() != null && v.getCapacidadCarga().compareTo(s.getPeso()) >= 0 && esVehiculoCompatible(v.getTipoVehiculo(), s.getTipoVehiculoRequerido())))
                .map(this::publicadaResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public SolicitudAceptadaResponse aceptarSolicitud(AceptarSolicitudRequest request) {
        Usuario usuario = usuarioActual();
        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.CONDUCTOR) throw new IllegalStateException("Solo un conductor puede aceptar solicitudes");
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId()).orElseThrow(() -> new EntityNotFoundException("No existe un conductor asociado a este usuario"));
        if (solicitudRepository.existsByConductorIdAndEstadoIn(conductor.getId(), ESTADOS_VIAJE_ACTIVO)) throw new IllegalStateException("Ya tienes un viaje activo. Finalízalo antes de aceptar otra solicitud");
        Solicitud solicitud = solicitudRepository.findById(request.getSolicitudId()).orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.PUBLICADA) throw new IllegalStateException("La solicitud no está disponible para aceptar");
        boolean vehiculoValido = vehiculoRepository.findAll().stream().filter(v -> Boolean.TRUE.equals(v.getActivo())).filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId())).anyMatch(v -> v.getCapacidadCarga() != null && v.getCapacidadCarga().compareTo(solicitud.getPeso()) >= 0 && esVehiculoCompatible(v.getTipoVehiculo(), solicitud.getTipoVehiculoRequerido()));
        if (!vehiculoValido) throw new IllegalStateException("El conductor no cuenta con un vehículo compatible para aceptar esta solicitud");
        conductor.setUbicacionLat(request.getLatitud());
        conductor.setUbicacionLng(request.getLongitud());
        conductor.setUbicacionActualizadaEn(LocalDateTime.now());
        conductorRepository.save(conductor);
        solicitud.setConductor(conductor);
        solicitud.setEstado(Solicitud.EstadoSolicitud.ACEPTADA);
        Solicitud aceptada = solicitudRepository.save(solicitud);
        registrarHistorial(aceptada, Solicitud.EstadoSolicitud.PUBLICADA.name(), Solicitud.EstadoSolicitud.ACEPTADA.name());
        BigDecimal monto = aceptada.getPrecioOfrecido();
        BigDecimal comision = monto.multiply(new BigDecimal("0.10"));
        Pago pago = Pago.builder().solicitud(aceptada).monto(monto).comisionPlataforma(comision).montoNetoConductor(monto.subtract(comision)).estado(Pago.EstadoPago.RETENIDO).fechaLimiteConfirmacion(aceptada.getFechaEntregaEstimada().plusDays(3)).build();
        Pago guardado = pagoRepository.save(pago);
        return new SolicitudAceptadaResponse(guardado.getId(), aceptada.getId(), conductor.getId(), aceptada.getDespachador().getUsuario().getTelefono(), aceptada.getEstado().name(), LocalDateTime.now(), "Solicitud aceptada correctamente");
    }

    @Override
    public SolicitudDetalleResponse obtenerSolicitudPorId(Long id) {
        Usuario usuario = usuarioActual();
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        if (usuario.getTipoUsuario() == Usuario.TipoUsuario.DESPACHADOR) {
            Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No existe un despachador asociado a este usuario"));
            if (!solicitud.getDespachador().getId().equals(despachador.getId())) {
                throw new AccessDeniedException("No tienes permiso para consultar esta solicitud");
            }
        } else if (usuario.getTipoUsuario() == Usuario.TipoUsuario.CONDUCTOR) {
            Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No existe un conductor asociado a este usuario"));
            boolean publicada = solicitud.getEstado() == Solicitud.EstadoSolicitud.PUBLICADA;
            boolean asignada = solicitud.getConductor() != null
                    && solicitud.getConductor().getId().equals(conductor.getId());
            if (!publicada && !asignada) {
                throw new AccessDeniedException("No tienes permiso para consultar esta solicitud");
            }
        } else {
            throw new AccessDeniedException("No tienes permiso para consultar esta solicitud");
        }
        return construirDetalle(solicitud);
    }

    @Override
    public SolicitudDetalleResponse obtenerSolicitudAceptadaActual() {
        Usuario usuario = usuarioActual();
        Solicitud solicitud;
        if (usuario.getTipoUsuario() == Usuario.TipoUsuario.DESPACHADOR) {
            Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No existe un despachador asociado a este usuario"));
            solicitud = solicitudRepository.findTopByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(
                    despachador.getId(),
                    List.of(Solicitud.EstadoSolicitud.ACEPTADA,
                            Solicitud.EstadoSolicitud.EN_CURSO,
                            Solicitud.EstadoSolicitud.COMPLETADA))
                    .orElseThrow(() -> new EntityNotFoundException("No tienes solicitudes aceptadas"));
        } else if (usuario.getTipoUsuario() == Usuario.TipoUsuario.CONDUCTOR) {
            Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No existe un conductor asociado a este usuario"));
            solicitud = solicitudRepository.findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(
                    conductor.getId(), ESTADOS_VIAJE_ACTIVO)
                    .orElseThrow(() -> new EntityNotFoundException("No tienes un viaje activo"));
        } else {
            throw new AccessDeniedException("No tienes permiso para consultar solicitudes aceptadas");
        }
        return construirDetalle(solicitud);
    }

    @Override
    public List<SolicitudDetalleResponse> listarFletesDespachador() {
        Usuario usuario = usuarioActual();
        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un despachador asociado a este usuario"));
        List<Solicitud.EstadoSolicitud> estados = List.of(
                Solicitud.EstadoSolicitud.PUBLICADA,
                Solicitud.EstadoSolicitud.ACEPTADA,
                Solicitud.EstadoSolicitud.EN_CURSO,
                Solicitud.EstadoSolicitud.COMPLETADA);
        return solicitudRepository.findByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(
                        despachador.getId(), estados)
                .stream()
                .map(this::construirDetalle)
                .filter(detalle -> !"LIBERADO".equals(detalle.getEstadoPago()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SolicitudDetalleResponse iniciarViaje(Long id) {
        Solicitud solicitud = obtenerSolicitudDelConductorActual(id);
        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.ACEPTADA) {
            throw new IllegalStateException("Solo puedes iniciar un viaje que esté ACEPTADO");
        }
        cambiarEstado(solicitud, Solicitud.EstadoSolicitud.EN_CURSO);
        return construirDetalle(solicitud);
    }

    @Override
    @Transactional
    public SolicitudDetalleResponse marcarEntregada(Long id) {
        Solicitud solicitud = obtenerSolicitudDelConductorActual(id);
        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.EN_CURSO) {
            throw new IllegalStateException("Solo puedes marcar como entregado un viaje EN CURSO");
        }
        cambiarEstado(solicitud, Solicitud.EstadoSolicitud.COMPLETADA);
        return construirDetalle(solicitud);
    }

    @Override
    @Transactional
    public SolicitudDetalleResponse confirmarEntrega(Long id) {
        Usuario usuario = usuarioActual();
        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un despachador asociado a este usuario"));
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (!solicitud.getDespachador().getId().equals(despachador.getId())) {
            throw new AccessDeniedException("No tienes permiso para confirmar esta solicitud");
        }
        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.COMPLETADA) {
            throw new IllegalStateException(
                    "Solo puedes confirmar la recepción cuando el conductor marque la entrega");
        }

        Pago pago = pagoRepository.findBySolicitudId(solicitud.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un pago para esta solicitud"));
        if (pago.getEstado() != Pago.EstadoPago.RETENIDO) {
            throw new IllegalStateException("El pago de esta solicitud ya fue procesado");
        }

        pago.setEstado(Pago.EstadoPago.LIBERADO);
        pago.setFechaLiberacion(LocalDateTime.now());
        pagoRepository.save(pago);
        return construirDetalle(solicitud);
    }

    private Usuario usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario autenticado no encontrado"));
    }

    private Solicitud obtenerSolicitudDelConductorActual(Long id) {
        Usuario usuario = usuarioActual();
        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe un conductor asociado a este usuario"));
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.getConductor() == null
                || !solicitud.getConductor().getId().equals(conductor.getId())) {
            throw new AccessDeniedException("Esta solicitud no está asignada a ti");
        }
        return solicitud;
    }

    private void cambiarEstado(Solicitud solicitud, Solicitud.EstadoSolicitud nuevoEstado) {
        String estadoAnterior = solicitud.getEstado().name();
        solicitud.setEstado(nuevoEstado);
        solicitudRepository.save(solicitud);
        registrarHistorial(solicitud, estadoAnterior, nuevoEstado.name());
    }

    private void registrarHistorial(Solicitud solicitud, String estadoAnterior, String estadoNuevo) {
        historialEstadoSolicitudRepository.save(HistorialEstadoSolicitud.builder()
                .solicitud(solicitud)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .fechaCambio(LocalDateTime.now())
                .build());
    }

    private SolicitudPublicadaResponse publicadaResponse(Solicitud solicitud) {
        return new SolicitudPublicadaResponse(
                solicitud.getId(),
                solicitud.getOrigen(),
                solicitud.getDestino(),
                solicitud.getTipoCarga(),
                solicitud.getTipoVehiculoRequerido(),
                solicitud.getPeso(),
                solicitud.getPrecioOfrecido(),
                solicitud.getFechaRecogida(),
                solicitud.getFechaEntregaEstimada(),
                solicitud.getRequiereCitaPuerto(),
                solicitud.getEstado().name(),
                solicitud.getFechaPublicacion(),
                solicitud.getDespachador().getId());
    }

    private SolicitudDetalleResponse construirDetalle(Solicitud s) {
        Usuario usuarioDespachador = s.getDespachador().getUsuario();
        Conductor conductor = s.getConductor();
        Usuario usuarioConductor = conductor != null ? conductor.getUsuario() : null;
        Pago pago = pagoRepository.findBySolicitudId(s.getId()).orElse(null);
        boolean compartirUbicacion = conductor != null && ESTADOS_VIAJE_ACTIVO.contains(s.getEstado());

        return new SolicitudDetalleResponse(
                s.getId(),
                s.getOrigen(),
                s.getDestino(),
                s.getOrigenLat(),
                s.getOrigenLng(),
                s.getDestinoLat(),
                s.getDestinoLng(),
                s.getTipoCarga(),
                s.getTipoVehiculoRequerido(),
                s.getPeso(),
                s.getPrecioOfrecido(),
                s.getFechaPublicacion(),
                s.getFechaRecogida(),
                s.getFechaEntregaEstimada(),
                s.getRequiereCitaPuerto(),
                s.getNumeroCita(),
                s.getEstado().name(),
                usuarioConductor != null ? usuarioDespachador.getNombre() : null,
                usuarioConductor != null ? usuarioDespachador.getTelefono() : null,
                usuarioConductor != null ? usuarioConductor.getNombre() : null,
                usuarioConductor != null ? usuarioConductor.getTelefono() : null,
                pago != null ? pago.getEstado().name() : null,
                pago != null ? pago.getMontoNetoConductor() : null,
                pago != null ? pago.getFechaLimiteConfirmacion() : null,
                compartirUbicacion ? conductor.getUbicacionLat() : null,
                compartirUbicacion ? conductor.getUbicacionLng() : null,
                compartirUbicacion ? conductor.getUbicacionActualizadaEn() : null);
    }

    private boolean esVehiculoCompatible(String vehiculo, String requerido) {
        if (vehiculo == null || requerido == null) {
            return false;
        }
        String vehiculoNormalizado = vehiculo.trim().toLowerCase();
        String requeridoNormalizado = requerido.trim().toLowerCase();
        return vehiculoNormalizado.equals(requeridoNormalizado)
                || requeridoNormalizado.contains(vehiculoNormalizado)
                || vehiculoNormalizado.contains(requeridoNormalizado);
    }
}
