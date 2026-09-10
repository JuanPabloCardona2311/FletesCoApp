package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDetalleResponse {

    private Long id;

    private String origen;
    private String destino;

    private BigDecimal origenLat;
    private BigDecimal origenLng;

    private BigDecimal destinoLat;
    private BigDecimal destinoLng;

    private String tipoCarga;
    private String tipoVehiculoRequerido;

    private BigDecimal peso;
    private BigDecimal precioOfrecido;

    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntregaEstimada;

    private Boolean requiereCitaPuerto;
    private String numeroCita;

    private String estado;
}