package com.example.msperfil.repository;

import com.example.msperfil.entity.DocumentoConductor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoConductorRepository extends JpaRepository<DocumentoConductor, Long> {
    List<DocumentoConductor> findByConductorId(Long conductorId);

    Optional<DocumentoConductor> findByConductorIdAndTipoDocumento(
            Long conductorId, DocumentoConductor.TipoDocumento tipoDocumento);
}
