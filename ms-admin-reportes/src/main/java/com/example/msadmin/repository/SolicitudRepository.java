package com.example.msadmin.repository;

import com.example.msadmin.entity.Solicitud;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    @Query("select s.estado, count(s) from Solicitud s group by s.estado")
    List<Object[]> contarPorEstado();

    // Últimas solicitudes, con filtro de estado opcional (GraphQL del administrador).
    @Query("select s from Solicitud s where (:estado is null or s.estado = :estado) order by s.fechaPublicacion desc")
    List<Solicitud> buscarRecientes(@Param("estado") Solicitud.EstadoSolicitud estado, Pageable pageable);
}
