package com.example.mssolicitudes.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SolicitudPublicadaResponse {
    private Long id; private String origen; private String destino; private String tipoCarga;
    private String tipoVehiculoRequerido; private BigDecimal peso; private BigDecimal precioOfrecido;
    private LocalDateTime fechaRecogida; private LocalDateTime fechaEntregaEstimada;
    private Boolean requiereCitaPuerto; private String estado; private LocalDateTime fechaPublicacion;
    private Long despachadorId;
}
