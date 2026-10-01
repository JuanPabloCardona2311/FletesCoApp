package com.example.mssolicitudes.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SolicitudAceptadaResponse {
    private Long pagoId; private Long solicitudId; private Long conductorId; private String telefonoContacto;
    private String estado; private LocalDateTime fechaAceptacion; private String mensaje;
}
