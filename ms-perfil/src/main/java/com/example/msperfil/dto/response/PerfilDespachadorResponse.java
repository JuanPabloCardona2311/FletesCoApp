package com.example.msperfil.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PerfilDespachadorResponse {
    private Long despachadorId;
    private String nombre;
    private String email;
    private String telefono;
    private String nombreEmpresa;
    private String nit;
    private Long totalSolicitudesPublicadas;
}
