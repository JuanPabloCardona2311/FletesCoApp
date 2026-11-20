package com.example.msadmin.repository;

import com.example.msadmin.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    List<Vehiculo> findByConductorIdOrderByIdAsc(Long conductorId);

    // Vehículos de varios conductores en una sola consulta (evita N+1 en GraphQL).
    List<Vehiculo> findByConductorIdIn(Collection<Long> conductorIds);
}
