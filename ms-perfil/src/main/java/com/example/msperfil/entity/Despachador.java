package com.example.msperfil.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "despachadores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Despachador {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @OneToOne(optional = true) @JoinColumn(name = "usuario_id", unique = true) @ToString.Exclude
    private Usuario usuario;
    @Column(name = "nombre_empresa", length = 150) private String nombreEmpresa;
    @Column(length = 20) private String nit;
}
