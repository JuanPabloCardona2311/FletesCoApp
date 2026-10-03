package com.example.msauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conductores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Conductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(optional = true)
    @JoinColumn(name = "usuario_id", unique = true)
    @ToString.Exclude
    private Usuario usuario;

    // Las relaciones con Vehiculo, DocumentoConductor y Notificacion
    // pertenecen a otros microservicios; se omiten aquí deliberadamente.

    @Column(name = "calificacion_promedio", precision = 3, scale = 2)
    private BigDecimal calificacionPromedio;

    @Column(name = "cancelaciones_totales", nullable = false)
    private Integer cancelacionesTotales;

    @Column(name = "ubicacion_lat", precision = 10, scale = 7)
    private BigDecimal ubicacionLat;

    @Column(name = "ubicacion_lng", precision = 10, scale = 7)
    private BigDecimal ubicacionLng;

    @Column(name = "ubicacion_actualizada_en")
    private LocalDateTime ubicacionActualizadaEn;
}
