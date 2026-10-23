package com.example.msauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RestablecerPasswordRequest {

    @NotBlank(message = "El enlace de recuperación no es válido")
    private String token;

    // Mismas reglas que la contraseña del registro (RegisterRequest).
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaPassword;
}
