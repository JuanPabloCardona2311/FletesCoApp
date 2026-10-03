package com.example.msauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tipoUsuario;
    private String email;
    private String nombre;

    public AuthResponse(String token, String tipoUsuario, String email) {
        this.token = token;
        this.tipoUsuario = tipoUsuario;
        this.email = email;
    }
}
