package com.example.msperfil.dto.response;

import com.example.msperfil.entity.Usuario.TipoUsuario;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DatosPersonalesResponse {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private TipoUsuario tipoUsuario;
}
