package com.example.msperfil.repository;

import com.example.msperfil.entity.Despachador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DespachadorRepository extends JpaRepository<Despachador, Long> {
    Optional<Despachador> findByUsuarioId(Long usuarioId);
}
