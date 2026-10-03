package com.example.msperfil.repository;

import com.example.msperfil.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    Optional<Conductor> findByUsuarioId(Long usuarioId);
}
