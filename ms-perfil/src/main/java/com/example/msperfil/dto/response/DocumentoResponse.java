package com.example.msperfil.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentoResponse {
    private Long id;
    /** CONDUCTOR (licencia) o VEHICULO (SOAT, tecnomecánica, tarjetas). */
    private String categoria;
    private String tipoDocumento;
    private Long vehiculoId;
    private String placa;
    private String estadoVerificacion;
    private LocalDate fechaVencimiento;
    private boolean vencido;
    /** Enlace firmado temporal al archivo; null si no se pudo generar. */
    private String enlaceVer;
    private Instant enlaceExpiraEn;
}
