package com.example.msperfil.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "conductores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class Conductor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @OneToOne(optional = true) @JoinColumn(name = "usuario_id", unique = true) @ToString.Exclude
    private Usuario usuario;
    @OneToMany(mappedBy = "conductor") @Builder.Default @ToString.Exclude
    private List<Vehiculo> vehiculos = new ArrayList<>();
    @OneToMany(mappedBy = "conductor") @Builder.Default @ToString.Exclude
    private List<DocumentoConductor> documentosConductor = new ArrayList<>();
    @Column(name = "calificacion_promedio", precision = 3, scale = 2) private BigDecimal calificacionPromedio;
    @Column(name = "cancelaciones_totales", nullable = false) private Integer cancelacionesTotales;
    @Column(name = "ubicacion_lat", precision = 10, scale = 7) private BigDecimal ubicacionLat;
    @Column(name = "ubicacion_lng", precision = 10, scale = 7) private BigDecimal ubicacionLng;
    @Column(name = "ubicacion_actualizada_en") private LocalDateTime ubicacionActualizadaEn;
}
