package com.example.mssolicitudes.repository;

import com.example.mssolicitudes.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    long countByDespachadorId(Long despachadorId);
    Optional<Solicitud> findTopByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(Long despachadorId, Collection<Solicitud.EstadoSolicitud> estados);
    Optional<Solicitud> findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(Long conductorId, Collection<Solicitud.EstadoSolicitud> estados);
    List<Solicitud> findByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(Long despachadorId, Collection<Solicitud.EstadoSolicitud> estados);
    boolean existsByConductorIdAndEstadoIn(Long conductorId, Collection<Solicitud.EstadoSolicitud> estados);
}
