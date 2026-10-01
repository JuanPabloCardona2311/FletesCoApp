package com.example.msperfil.dto.response;

import java.math.BigDecimal;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VehiculoPerfilResponse {
    private Long id;
    private String tipoVehiculo;
    private String placa;
    private BigDecimal capacidadCarga;
    private String estadoVerificacion;
    private Boolean activo;
}
