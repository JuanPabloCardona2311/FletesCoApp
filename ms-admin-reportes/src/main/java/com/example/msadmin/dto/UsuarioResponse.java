package com.example.msadmin.dto;

import com.example.msadmin.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        Usuario.TipoUsuario tipoUsuario,
        Usuario.EstadoUsuario estado,
        LocalDateTime fechaRegistro) {
}
