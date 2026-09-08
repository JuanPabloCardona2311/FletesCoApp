package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDespachadorResponse {
    private Long despachadorId;
    private String nombre;
    private String email;
    private String telefono;
    private String nombreEmpresa;
    private String nit;
    private Long totalSolicitudesPublicadas;
}
