package com.example.demo.repository;
import com.example.demo.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    long countByDespachadorId(Long despachadorId);
}
