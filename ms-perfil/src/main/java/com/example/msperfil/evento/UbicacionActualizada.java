package com.example.msperfil.evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Evento interno: el conductor guardó una nueva ubicación.
public record UbicacionActualizada(Long conductorId, BigDecimal latitud, BigDecimal longitud, LocalDateTime fecha) {
}
