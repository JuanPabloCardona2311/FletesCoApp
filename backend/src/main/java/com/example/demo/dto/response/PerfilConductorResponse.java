package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilConductorResponse {
    private Long conductorId;
    private String nombre;
    private String email;
    private String telefono;
    private BigDecimal calificacionPromedio;
    private Integer cancelacionesTotales;
    private BigDecimal ubicacionLat;
    private BigDecimal ubicacionLng;
    private List<VehiculoPerfilResponse> vehiculos;
}
