package com.example.msadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

// Solo lectura: la tabla la administra ms-perfil. Se usa en el GraphQL del administrador.
// equals/hashCode por id porque @BatchMapping usa los conductores como llaves de un Map.
@Entity
@Immutable
@Table(name = "conductores")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Conductor {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "calificacion_promedio", precision = 3, scale = 2)
    private BigDecimal calificacionPromedio;

    @Column(name = "cancelaciones_totales", nullable = false)
    private Integer cancelacionesTotales;
}
