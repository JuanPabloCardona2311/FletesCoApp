package com.example.mssolicitudes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity @Table(name = "vehiculos") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Vehiculo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "conductor_id", nullable = false) @ToString.Exclude private Conductor conductor;
    @Column(name = "tipo_vehiculo", nullable = false, length = 50) private String tipoVehiculo;
    @Column(nullable = false, length = 10) private String placa;
    @Column(name = "capacidad_carga", nullable = false, precision = 10, scale = 2) private BigDecimal capacidadCarga;
    @Builder.Default @Enumerated(EnumType.STRING) @Column(name = "estado_verificacion", nullable = false, length = 10) private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;
    @Column(nullable = false) private Boolean activo;
    public enum EstadoVerificacion {
        PENDIENTE,
        VERIFICADO,
        RECHAZADO
    }
}
