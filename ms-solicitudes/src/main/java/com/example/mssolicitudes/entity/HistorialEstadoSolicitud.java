package com.example.mssolicitudes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name = "historial_estados_solicitud") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class HistorialEstadoSolicitud {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "solicitud_id", nullable = false) @ToString.Exclude private Solicitud solicitud;
    @Column(name = "estado_anterior", length = 50) private String estadoAnterior;
    @Column(name = "estado_nuevo", nullable = false, length = 50) private String estadoNuevo;
    @Column(name = "fecha_cambio", nullable = false) private LocalDateTime fechaCambio;
}
