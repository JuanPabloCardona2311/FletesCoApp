package com.example.msadmin.dto;

import java.util.Map;

public record ResumenReportesResponse(
        Map<String, Long> solicitudesPorEstado,
        Map<String, Long> disputasPorEstado,
        Map<String, Long> usuariosPorEstado,
        Map<String, Long> usuariosPorTipo,
        long totalSolicitudes) {
}
