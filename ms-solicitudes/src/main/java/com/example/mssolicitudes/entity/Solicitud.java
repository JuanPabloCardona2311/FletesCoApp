package com.example.mssolicitudes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "solicitudes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Solicitud {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "despachador_id", nullable = false) @ToString.Exclude
    private Despachador despachador;
    @ManyToOne @JoinColumn(name = "conductor_id") @ToString.Exclude
    private Conductor conductor;
    @OneToOne(mappedBy = "solicitud") @ToString.Exclude private Pago pago;
    @OneToMany(mappedBy = "solicitud") @Builder.Default @ToString.Exclude
    private List<HistorialEstadoSolicitud> historialEstados = new ArrayList<>();
    @Column(nullable = false, length = 200) private String origen;
    @Column(nullable = false, length = 200) private String destino;
    @Column(name = "origen_lat", nullable = false, precision = 10, scale = 7) private BigDecimal origenLat;
    @Column(name = "origen_lng", nullable = false, precision = 10, scale = 7) private BigDecimal origenLng;
    @Column(name = "destino_lat", nullable = false, precision = 10, scale = 7) private BigDecimal destinoLat;
    @Column(name = "destino_lng", nullable = false, precision = 10, scale = 7) private BigDecimal destinoLng;
    @Column(name = "tipo_carga", nullable = false, length = 100) private String tipoCarga;
    @Column(name = "tipo_vehiculo_requerido", nullable = false, length = 50) private String tipoVehiculoRequerido;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal peso;
    @Column(name = "precio_ofrecido", nullable = false, precision = 12, scale = 2) private BigDecimal precioOfrecido;
    @Column(name = "fecha_publicacion", nullable = false) private LocalDateTime fechaPublicacion;
    @Column(name = "fecha_recogida", nullable = false) private LocalDateTime fechaRecogida;
    @Column(name = "fecha_entrega_estimada", nullable = false) private LocalDateTime fechaEntregaEstimada;
    @Column(name = "requiere_cita_puerto", nullable = false) private Boolean requiereCitaPuerto;
    @Column(name = "numero_cita", length = 50) private String numeroCita;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private EstadoSolicitud estado;
    public enum EstadoSolicitud {
        PUBLICADA,
        ACEPTADA,
        EN_CURSO,
        COMPLETADA,
        CANCELADA,
        EXPIRADA
    }
}
