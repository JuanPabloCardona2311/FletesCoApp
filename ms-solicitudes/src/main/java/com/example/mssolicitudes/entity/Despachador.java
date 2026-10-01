package com.example.mssolicitudes.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity @Table(name = "despachadores") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Despachador {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @OneToOne(optional = true) @JoinColumn(name = "usuario_id", unique = true) @ToString.Exclude private Usuario usuario;
    @OneToMany(mappedBy = "despachador") @Builder.Default @ToString.Exclude private List<Solicitud> solicitudes = new ArrayList<>();
    @Column(name = "nombre_empresa", length = 150) private String nombreEmpresa;
    @Column(length = 20) private String nit;
}
