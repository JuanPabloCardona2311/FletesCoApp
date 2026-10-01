package com.example.mssolicitudes.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AceptarSolicitudRequest {
    @NotNull private Long solicitudId;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private BigDecimal latitud;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private BigDecimal longitud;
}
