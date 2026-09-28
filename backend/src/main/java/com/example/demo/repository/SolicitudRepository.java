package com.example.demo.repository;
import com.example.demo.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    long countByDespachadorId(Long despachadorId);

    Optional<Solicitud> findTopByDespachadorIdAndEstadoInOrderByFechaPublicacionDesc(
            Long despachadorId, Collection<Solicitud.EstadoSolicitud> estados);

    Optional<Solicitud> findTopByConductorIdAndEstadoInOrderByFechaPublicacionDesc(
            Long conductorId, Collection<Solicitud.EstadoSolicitud> estados);
}
