package com.example.msauth.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class AuthResponse {
    private boolean ok;
    private String token;
    private String tipoUsuario;

    public AuthResponse(boolean ok, String token, String tipoUsuario) {
        this.ok = ok;
        this.token = token;
        this.tipoUsuario = tipoUsuario;
    }
}
