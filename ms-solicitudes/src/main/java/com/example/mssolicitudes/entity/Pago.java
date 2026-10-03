package com.example.mssolicitudes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name = "pagos") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "solicitud_id", nullable = false, unique = true) @ToString.Exclude private Solicitud solicitud;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal monto;
    @Column(name = "comision_plataforma", nullable = false, precision = 12, scale = 2) private BigDecimal comisionPlataforma;
    @Column(name = "monto_neto_conductor", nullable = false, precision = 12, scale = 2) private BigDecimal montoNetoConductor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoPago estado;
    @Column(name = "referencia_pasarela", length = 100) private String referenciaPasarela;
    @Column(name = "foto_evidencia_entrega", length = 255) private String fotoEvidenciaEntrega;
    @Column(name = "fecha_limite_confirmacion", nullable = false) private LocalDateTime fechaLimiteConfirmacion;
    @Column(name = "fecha_pago") private LocalDateTime fechaPago;
    @Column(name = "fecha_liberacion") private LocalDateTime fechaLiberacion;
    public enum EstadoPago {
        PENDIENTE,
        RETENIDO,
        LIBERADO,
        REEMBOLSADO
    }
}
