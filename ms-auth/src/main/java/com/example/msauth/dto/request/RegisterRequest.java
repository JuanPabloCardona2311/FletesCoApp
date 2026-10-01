package com.example.msauth.dto.request;

import com.example.msauth.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^3[0-9]{9}$", message = "Debe ser un celular colombiano válido (10 dígitos, inicia en 3)")
    private String telefono;

    @NotNull(message = "Debe indicar el tipo de documento")
    private Usuario.TipoDocumentoIdentidad tipoDocumentoIdentidad;

    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "^[0-9]{5,20}$", message = "El documento debe tener entre 5 y 20 dígitos, solo números")
    private String numeroDocumentoIdentidad;

    @NotNull(message = "Debe indicar el tipo de usuario")
    private Usuario.TipoUsuario tipoUsuario;
}
