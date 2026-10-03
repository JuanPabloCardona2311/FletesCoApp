package com.example.msadmin.dto;

import com.example.msadmin.entity.Disputa;

import java.time.LocalDateTime;

public record DisputaResponse(
        Long id,
        Long solicitudId,
        Long usuarioReportaId,
        String usuarioReportaNombre,
        String motivo,
        Disputa.EstadoDisputa estado,
        String resolucion,
        LocalDateTime fechaApertura,
        LocalDateTime fechaResolucion) {
}
