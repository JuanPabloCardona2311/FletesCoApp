package com.example.msadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "solicitudes")
@Getter
public class Solicitud {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String origen;

    @Column(nullable = false, length = 200)
    private String destino;

    @Column(name = "precio_ofrecido", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioOfrecido;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_recogida", nullable = false)
    private LocalDateTime fechaRecogida;

    @Column(name = "fecha_entrega_estimada", nullable = false)
    private LocalDateTime fechaEntregaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoSolicitud estado;

    public enum EstadoSolicitud {
        PUBLICADA,
        ACEPTADA,
        EN_CURSO,
        COMPLETADA,
        CANCELADA,
        EXPIRADA
    }
}
