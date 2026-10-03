package com.example.msadmin.service.impl;

import com.example.msadmin.dto.DisputaResponse;
import com.example.msadmin.entity.Disputa;
import com.example.msadmin.repository.DisputaRepository;
import com.example.msadmin.service.DisputaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisputaServiceImpl implements DisputaService {

    private final DisputaRepository disputaRepository;

    public DisputaServiceImpl(DisputaRepository disputaRepository) {
        this.disputaRepository = disputaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputaResponse> listar(Disputa.EstadoDisputa estado) {
        return disputaRepository.buscarPorEstado(estado).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DisputaResponse resolver(Long id, String resolucion) {
        Disputa disputa = disputaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Disputa no encontrada"));
        if (disputa.getEstado() == Disputa.EstadoDisputa.RESUELTA) {
            throw new IllegalStateException("La disputa ya está RESUELTA");
        }
        disputa.setEstado(Disputa.EstadoDisputa.RESUELTA);
        disputa.setResolucion(resolucion);
        disputa.setFechaResolucion(LocalDateTime.now());
        return toResponse(disputaRepository.save(disputa));
    }

    private DisputaResponse toResponse(Disputa disputa) {
        return new DisputaResponse(
                disputa.getId(),
                disputa.getSolicitud().getId(),
                disputa.getUsuarioReporta().getId(),
                disputa.getUsuarioReporta().getNombre(),
                disputa.getMotivo(),
                disputa.getEstado(),
                disputa.getResolucion(),
                disputa.getFechaApertura(),
                disputa.getFechaResolucion());
    }
}
