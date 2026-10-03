package com.example.msadmin.repository;

import com.example.msadmin.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    @Query("select s.estado, count(s) from Solicitud s group by s.estado")
    List<Object[]> contarPorEstado();
}
