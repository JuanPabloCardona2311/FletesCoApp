package com.example.demo.dto.request;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PublicarSolicitudRequest {

    @NotBlank(message = "El origen es obligatorio")
    @Size(max = 200, message = "El origen no puede superar 200 caracteres")
    private String origen;

    @NotBlank(message = "El destino es obligatorio")
    @Size(max = 200, message = "El destino no puede superar 200 caracteres")
    private String destino;

    @NotNull(message = "La latitud de origen es obligatoria")
    @DecimalMin(value = "-90", message = "La latitud de origen debe ser mayor o igual a -90")
    @DecimalMax(value = "90", message = "La latitud de origen debe ser menor o igual a 90")
    private BigDecimal origenLat;

    @NotNull(message = "La longitud de origen es obligatoria")
    @DecimalMin(value = "-180", message = "La longitud de origen debe ser mayor o igual a -180")
    @DecimalMax(value = "180", message = "La longitud de origen debe ser menor o igual a 180")
    private BigDecimal origenLng;

    @NotNull(message = "La latitud de destino es obligatoria")
    @DecimalMin(value = "-90", message = "La latitud de destino debe ser mayor o igual a -90")
    @DecimalMax(value = "90", message = "La latitud de destino debe ser menor o igual a 90")
    private BigDecimal destinoLat;

    @NotNull(message = "La longitud de destino es obligatoria")
    @DecimalMin(value = "-180", message = "La longitud de destino debe ser mayor o igual a -180")
    @DecimalMax(value = "180", message = "La longitud de destino debe ser menor o igual a 180")
    private BigDecimal destinoLng;

    @NotBlank(message = "El tipo de carga es obligatorio")
    @Size(max = 100, message = "El tipo de carga no puede superar 100 caracteres")
    private String tipoCarga;

    @NotBlank(message = "El tipo de vehículo requerido es obligatorio")
    @Size(max = 50, message = "El tipo de vehículo no puede superar 50 caracteres")
    @Pattern(regexp = "Camioneta|Camión|Mula", message = "El tipo de vehículo debe ser Camioneta, Camión o Mula")
    private String tipoVehiculoRequerido;

    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
    @Digits(integer = 8, fraction = 2, message = "El peso puede tener hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal peso;

    @NotNull(message = "El precio ofrecido es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio ofrecido debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio puede tener hasta 10 dígitos enteros y 2 decimales")
    private BigDecimal precioOfrecido;

    @NotNull(message = "La fecha de recogida es obligatoria")
    @Future(message = "La fecha de recogida debe ser una fecha futura")
    private LocalDateTime fechaRecogida;

    @NotNull(message = "La fecha estimada de entrega es obligatoria")
    private LocalDateTime fechaEntregaEstimada;

    @NotNull(message = "Debe indicar si requiere cita en puerto")
    private Boolean requiereCitaPuerto;

    @Size(max = 50, message = "El número de cita no puede superar 50 caracteres")
    private String numeroCita;
}
