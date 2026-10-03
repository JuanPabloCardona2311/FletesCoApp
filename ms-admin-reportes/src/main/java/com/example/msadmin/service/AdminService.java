package com.example.msadmin.service;

import com.example.msadmin.dto.UsuarioResponse;
import com.example.msadmin.entity.Usuario;

import java.util.List;

public interface AdminService {

    List<UsuarioResponse> listarUsuarios(Usuario.TipoUsuario tipo, Usuario.EstadoUsuario estado);

    UsuarioResponse cambiarEstado(Long id, Usuario.EstadoUsuario estado, String emailAdministrador);
}
