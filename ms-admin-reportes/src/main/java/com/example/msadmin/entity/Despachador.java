package com.example.msadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

// Solo lectura: la tabla la administra ms-perfil. Se usa en el GraphQL del administrador.
@Entity
@Immutable
@Table(name = "despachadores")
@Getter
public class Despachador {

    @Id
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_empresa", length = 150)
    private String nombreEmpresa;

    @Column(length = 20)
    private String nit;
}
