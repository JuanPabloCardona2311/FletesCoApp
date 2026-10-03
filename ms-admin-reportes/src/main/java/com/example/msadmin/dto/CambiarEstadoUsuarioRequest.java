package com.example.msadmin.dto;

import com.example.msadmin.entity.Usuario;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(@NotNull Usuario.EstadoUsuario estado) {
}
