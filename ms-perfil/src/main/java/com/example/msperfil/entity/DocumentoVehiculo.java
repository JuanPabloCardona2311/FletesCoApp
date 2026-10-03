package com.example.msperfil.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "documentos_vehiculo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public class DocumentoVehiculo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id", nullable = false) @ToString.Exclude private Vehiculo vehiculo;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_documento", nullable = false, length = 30) private TipoDocumento tipoDocumento;
    @Column(name = "archivo_url", nullable = false, length = 255) private String archivoUrl;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;
    @Enumerated(EnumType.STRING) @Column(name = "estado_verificacion", nullable = false, length = 20) private EstadoVerificacion estadoVerificacion;
    public enum TipoDocumento { SOAT, TECNOMECANICA, TARJETA_PROPIEDAD, TARJETA_OPERACION }
    public enum EstadoVerificacion { PENDIENTE, VERIFICADO, RECHAZADO }
}
