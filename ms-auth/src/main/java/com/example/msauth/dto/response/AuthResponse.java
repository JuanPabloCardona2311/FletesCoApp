package com.example.msauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Respuesta de login y registro. El JWT no va aquí: viaja solo en la cookie HttpOnly
// FLETESCO_TOKEN, que JavaScript no puede leer.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String tipoUsuario;
    private String email;
    private String nombre;
}
