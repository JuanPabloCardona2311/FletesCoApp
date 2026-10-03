package com.example.mssolicitudes.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PublicarSolicitudRequest {
    @NotBlank @Size(max = 200) private String origen;
    @NotBlank @Size(max = 200) private String destino;
    @NotNull @DecimalMin("-90") @DecimalMax("90") private BigDecimal origenLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private BigDecimal origenLng;
    @NotNull @DecimalMin("-90") @DecimalMax("90") private BigDecimal destinoLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private BigDecimal destinoLng;
    @NotBlank @Size(max = 100) private String tipoCarga;
    @NotBlank @Size(max = 50) @Pattern(regexp = "Camioneta|Camión|Mula") private String tipoVehiculoRequerido;
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) private BigDecimal peso;
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) private BigDecimal precioOfrecido;
    @NotNull @Future private LocalDateTime fechaRecogida;
    @NotNull private LocalDateTime fechaEntregaEstimada;
    @NotNull private Boolean requiereCitaPuerto;
    @Size(max = 50) private String numeroCita;
}
