package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoPerfilResponse {
    private Long id;
    private String tipoVehiculo;
    private String placa;
    private BigDecimal capacidadCarga;
    private String estadoVerificacion;
    private Boolean activo;
}
