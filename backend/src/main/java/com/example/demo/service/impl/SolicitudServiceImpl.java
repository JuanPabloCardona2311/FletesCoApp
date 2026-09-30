package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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

        private static final List<Solicitud.EstadoSolicitud> ESTADOS_VIAJE_ACTIVO = List.of(
                        Solicitud.EstadoSolicitud.ACEPTADA,
                        Solicitud.EstadoSolicitud.EN_CURSO);

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
                        HistorialEstadoSolicitudRepository historialEstadoSolicitudRepository) {
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "No existe un despachador asociado a este usuario"));

                if (!request.getFechaEntregaEstimada().isAfter(request.getFechaRecogida())) {
                        throw new IllegalStateException(
                                        "La fecha de entrega debe ser posterior a la fecha de recogida");
                }

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
                                guardada.getPeso(),
                                guardada.getPrecioOfrecido(),
                                guardada.getFechaRecogida(),
                                guardada.getFechaEntregaEstimada(),
                                guardada.getRequiereCitaPuerto(),
                                guardada.getEstado().name(),
                                guardada.getFechaPublicacion(),
                                guardada.getDespachador().getId());
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "No existe un conductor asociado a este usuario"));

                List<Vehiculo> vehiculos = vehiculoRepository.findAll().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                                .filter(v -> v.getConductor() != null
                                                && v.getConductor().getId().equals(conductor.getId()))
                                .collect(Collectors.toList());

                if (vehiculos.isEmpty()) {
                        return List.of();
                }

                return solicitudRepository.findAll().stream()
                                .filter(s -> s.getEstado() == Solicitud.EstadoSolicitud.PUBLICADA)
                                .filter(s -> vehiculos.stream().anyMatch(v -> v.getCapacidadCarga() != null &&
                                                v.getCapacidadCarga().compareTo(s.getPeso()) >= 0 &&
                                                esVehiculoCompatible(v.getTipoVehiculo(),
                                                                s.getTipoVehiculoRequerido())))
                                .map(s -> new SolicitudPublicadaResponse(
                                                s.getId(),
                                                s.getOrigen(),
                                                s.getDestino(),
                                                s.getTipoCarga(),
                                                s.getTipoVehiculoRequerido(),
                                                s.getPeso(),
                                                s.getPrecioOfrecido(),
                                                s.getFechaRecogida(),
                                                s.getFechaEntregaEstimada(),
                                                s.getRequiereCitaPuerto(),
                                                s.getEstado().name(),
                                                s.getFechaPublicacion(),
                                                s.getDespachador().getId()))
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "No existe un conductor asociado a este usuario"));

                if (solicitudRepository.existsByConductorIdAndEstadoIn(conductor.getId(), ESTADOS_VIAJE_ACTIVO)) {
                        throw new IllegalStateException(
                                        "Ya tienes un viaje activo. Finalízalo antes de aceptar otra solicitud");
                }

                Solicitud solicitud = solicitudRepository.findById(request.getSolicitudId())
                                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

                if (solicitud.getEstado() != Solicitud.EstadoSolicitud.PUBLICADA) {
                        throw new IllegalStateException("La solicitud no está disponible para aceptar");
                }

                boolean tieneVehiculoValido = vehiculoRepository.findAll().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                                .filter(v -> v.getConductor() != null
                                                && v.getConductor().getId().equals(conductor.getId()))
                                .anyMatch(v -> v.getCapacidadCarga() != null &&
                                                v.getCapacidadCarga().compareTo(solicitud.getPeso()) >= 0 &&
                                                esVehiculoCompatible(v.getTipoVehiculo(),
                                                                solicitud.getTipoVehiculoRequerido()));

                if (!tieneVehiculoValido) {
                        throw new IllegalStateException(
                                        "El conductor no cuenta con un vehículo compatible para aceptar esta solicitud");
                }

                // Ubicación inicial: el despachador ve al conductor desde el momento en que acepta.
                conductor.setUbicacionLat(request.getLatitud());
                conductor.setUbicacionLng(request.getLongitud());
                conductor.setUbicacionActualizadaEn(LocalDateTime.now());
                conductorRepository.save(conductor);

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
                                "Solicitud aceptada correctamente");
        }

        @Override
        public SolicitudDetalleResponse obtenerSolicitudPorId(Long id) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Usuario autenticado no encontrado"));

                Solicitud solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Solicitud no encontrada"));

                if (usuario.getTipoUsuario() == Usuario.TipoUsuario.DESPACHADOR) {

                        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No existe un despachador asociado a este usuario"));

                        boolean esPropietario = solicitud.getDespachador()
                                        .getId()
                                        .equals(despachador.getId());

                        if (!esPropietario) {
                                throw new AccessDeniedException(
                                                "No tienes permiso para consultar esta solicitud");
                        }

                } else if (usuario.getTipoUsuario() == Usuario.TipoUsuario.CONDUCTOR) {

                        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No existe un conductor asociado a este usuario"));

                        boolean estaPublicada = solicitud.getEstado() == Solicitud.EstadoSolicitud.PUBLICADA;

                        boolean estaAsignadaAlConductor = solicitud.getConductor() != null
                                        && solicitud.getConductor()
                                                        .getId()
                                                        .equals(conductor.getId());

                        if (!estaPublicada && !estaAsignadaAlConductor) {
                                throw new AccessDeniedException(
                                                "No tienes permiso para consultar esta solicitud");
                        }

                } else {

                        throw new AccessDeniedException(
                                        "No tienes permiso para consultar esta solicitud");
                }

                return construirDetalle(solicitud);
        }

        @Override
        public SolicitudDetalleResponse obtenerSolicitudAceptadaActual() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Usuario autenticado no encontrado"));

                Solicitud solicitud;
                if (usuario.getTipoUsuario() == Usuario.TipoUsuario.DESPACHADOR) {
                        // El despachador también ve las COMPLETADA: debe confirmar la entrega para liberar el pago.
                        List<Solicitud.EstadoSolicitud> estadosDespachador = Arrays.asList(
                                        Solicitud.EstadoSolicitud.ACEPTADA,
                                        Solicitud.EstadoSolicitud.EN_CURSO,
                                        Solicitud.EstadoSolicitud.COMPLETADA);
                        Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No existe un despachador asociado a este usuario"));
                        solicitud = solicitudRepository
                                        .findTopByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(
                                                        despachador.getId(), estadosDespachador)
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No tienes solicitudes aceptadas"));
                } else if (usuario.getTipoUsuario() == Usuario.TipoUsuario.CONDUCTOR) {
                        Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No existe un conductor asociado a este usuario"));
                        solicitud = solicitudRepository
                                        .findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(
                                                        conductor.getId(), ESTADOS_VIAJE_ACTIVO)
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "No tienes un viaje activo"));
                } else {
                        throw new AccessDeniedException(
                                        "No tienes permiso para consultar solicitudes aceptadas");
                }

                return construirDetalle(solicitud);
        }

        @Override
        public List<SolicitudDetalleResponse> listarFletesDespachador() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

                Despachador despachador = despachadorRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "No existe un despachador asociado a este usuario"));

                List<Solicitud.EstadoSolicitud> estadosEnSeguimiento = List.of(
                                Solicitud.EstadoSolicitud.PUBLICADA,
                                Solicitud.EstadoSolicitud.ACEPTADA,
                                Solicitud.EstadoSolicitud.EN_CURSO,
                                Solicitud.EstadoSolicitud.COMPLETADA);

                // Un flete sale del seguimiento cuando el despachador confirma la recepción (pago LIBERADO).
                return solicitudRepository
                                .findByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(
                                                despachador.getId(), estadosEnSeguimiento)
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
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

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
                                .orElseThrow(() -> new EntityNotFoundException("No existe un pago para esta solicitud"));

                if (pago.getEstado() != Pago.EstadoPago.RETENIDO) {
                        throw new IllegalStateException("El pago de esta solicitud ya fue procesado");
                }

                pago.setEstado(Pago.EstadoPago.LIBERADO);
                pago.setFechaLiberacion(LocalDateTime.now());
                pagoRepository.save(pago);

                return construirDetalle(solicitud);
        }

        private Solicitud obtenerSolicitudDelConductorActual(Long solicitudId) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

                Conductor conductor = conductorRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "No existe un conductor asociado a este usuario"));

                Solicitud solicitud = solicitudRepository.findById(solicitudId)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

                if (solicitud.getConductor() == null
                                || !solicitud.getConductor().getId().equals(conductor.getId())) {
                        throw new AccessDeniedException("Esta solicitud no está asignada a ti");
                }

                return solicitud;
        }

        private void cambiarEstado(Solicitud solicitud, Solicitud.EstadoSolicitud nuevoEstado) {
                Solicitud.EstadoSolicitud estadoAnterior = solicitud.getEstado();
                solicitud.setEstado(nuevoEstado);
                solicitudRepository.save(solicitud);

                historialEstadoSolicitudRepository.save(HistorialEstadoSolicitud.builder()
                                .solicitud(solicitud)
                                .estadoAnterior(estadoAnterior.name())
                                .estadoNuevo(nuevoEstado.name())
                                .fechaCambio(LocalDateTime.now())
                                .build());
        }

        private SolicitudDetalleResponse construirDetalle(Solicitud solicitud) {
                Usuario usuarioDespachador = solicitud.getDespachador().getUsuario();
                Conductor conductorAsignado = solicitud.getConductor();
                Usuario usuarioConductor = conductorAsignado != null ? conductorAsignado.getUsuario() : null;
                Pago pago = pagoRepository.findBySolicitudId(solicitud.getId()).orElse(null);

                // La ubicación del conductor solo se expone mientras el viaje está activo.
                boolean compartirUbicacion = conductorAsignado != null
                                && ESTADOS_VIAJE_ACTIVO.contains(solicitud.getEstado());

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

                                solicitud.getEstado().name(),

                                // El contacto del despachador solo se comparte una vez asignado un conductor.
                                usuarioConductor != null ? usuarioDespachador.getNombre() : null,
                                usuarioConductor != null ? usuarioDespachador.getTelefono() : null,
                                usuarioConductor != null ? usuarioConductor.getNombre() : null,
                                usuarioConductor != null ? usuarioConductor.getTelefono() : null,

                                pago != null ? pago.getEstado().name() : null,
                                pago != null ? pago.getMontoNetoConductor() : null,
                                pago != null ? pago.getFechaLimiteConfirmacion() : null,

                                compartirUbicacion ? conductorAsignado.getUbicacionLat() : null,
                                compartirUbicacion ? conductorAsignado.getUbicacionLng() : null,
                                compartirUbicacion ? conductorAsignado.getUbicacionActualizadaEn() : null);
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
