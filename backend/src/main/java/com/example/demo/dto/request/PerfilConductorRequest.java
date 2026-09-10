package com.example.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilConductorRequest {

    @NotBlank(message = "El tipo de vehiculo es obligatorio")
    @Size(max = 50, message = "El tipo de vehiculo no puede superar 50 caracteres")
    private String tipoVehiculo;

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 10, message = "La placa no puede superar 10 caracteres")
    private String placa;

    @NotNull(message = "La capacidad de carga es obligatoria")
    @DecimalMin(value = "0.01", message = "La capacidad de carga debe ser mayor a cero")
    private BigDecimal capacidadCarga;

    private BigDecimal ubicacionLat;

    private BigDecimal ubicacionLng;
}
