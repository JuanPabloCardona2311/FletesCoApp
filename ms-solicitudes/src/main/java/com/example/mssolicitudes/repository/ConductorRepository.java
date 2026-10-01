package com.example.mssolicitudes.repository;

import com.example.mssolicitudes.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    Optional<Conductor> findByUsuarioId(Long usuarioId);
}
