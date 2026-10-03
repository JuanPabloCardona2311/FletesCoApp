package com.example.msperfil.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PerfilDespachadorRequest {
    @Size(max = 150, message = "El nombre de empresa no puede superar 150 caracteres")
    private String nombreEmpresa;
    @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
    private String nit;
}
