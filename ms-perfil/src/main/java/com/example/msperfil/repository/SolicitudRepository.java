package com.example.msperfil.repository;

import com.example.msperfil.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    long countByDespachadorId(Long despachadorId);

    boolean existsByConductorIdAndEstadoIn(
            Long conductorId,
            Collection<Solicitud.EstadoSolicitud> estados);
}
