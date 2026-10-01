package com.example.mssolicitudes.repository;

import com.example.mssolicitudes.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findBySolicitudId(Long solicitudId);
}
