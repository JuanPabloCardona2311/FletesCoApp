package com.example.mssolicitudes.repository;

import com.example.mssolicitudes.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByConductorId(Long conductorId);
}
