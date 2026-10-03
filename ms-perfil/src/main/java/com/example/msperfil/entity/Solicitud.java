package com.example.msperfil.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "solicitudes")
@Getter @NoArgsConstructor
public class Solicitud {
    @Id
    private Long id;
    @Column(name = "despachador_id", nullable = false)
    private Long despachadorId;
    @Column(name = "conductor_id")
    private Long conductorId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoSolicitud estado;

    public enum EstadoSolicitud { PUBLICADA, ACEPTADA, EN_CURSO, COMPLETADA, CANCELADA, EXPIRADA }
}
