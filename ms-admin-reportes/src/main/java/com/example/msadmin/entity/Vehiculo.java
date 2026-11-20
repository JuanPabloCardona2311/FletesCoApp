package com.example.msadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

// Solo lectura: la tabla la administra ms-perfil. Se usa en el GraphQL del administrador.
@Entity
@Immutable
@Table(name = "vehiculos")
@Getter
public class Vehiculo {

    @Id
    private Long id;

    @Column(name = "conductor_id", nullable = false)
    private Long conductorId;

    @Column(name = "tipo_vehiculo", nullable = false, length = 50)
    private String tipoVehiculo;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(name = "capacidad_carga", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadCarga;

    // Texto (PENDIENTE, VERIFICADO o RECHAZADO): este servicio solo lo muestra.
    @Column(name = "estado_verificacion", nullable = false, length = 10)
    private String estadoVerificacion;

    @Column(nullable = false)
    private Boolean activo;
}
