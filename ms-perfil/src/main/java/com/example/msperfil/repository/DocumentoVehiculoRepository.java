package com.example.msperfil.repository;

import com.example.msperfil.entity.DocumentoVehiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoVehiculoRepository extends JpaRepository<DocumentoVehiculo, Long> {
    // Documentos de todos los vehículos de un conductor (vehiculo.conductor.id).
    List<DocumentoVehiculo> findByVehiculo_Conductor_Id(Long conductorId);

    Optional<DocumentoVehiculo> findByVehiculoIdAndTipoDocumento(
            Long vehiculoId, DocumentoVehiculo.TipoDocumento tipoDocumento);
}
