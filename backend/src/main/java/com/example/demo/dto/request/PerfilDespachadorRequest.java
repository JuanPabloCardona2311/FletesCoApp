package com.example.demo.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDespachadorRequest {

    @Size(max = 150, message = "El nombre de empresa no puede superar 150 caracteres")
    private String nombreEmpresa;

    @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
    private String nit;
}
