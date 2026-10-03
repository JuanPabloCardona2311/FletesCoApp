package com.example.msadmin.service.impl;

import com.example.msadmin.dto.UsuarioResponse;
import com.example.msadmin.entity.Usuario;
import com.example.msadmin.repository.UsuarioRepository;
import com.example.msadmin.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final UsuarioRepository usuarioRepository;

    public AdminServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios(Usuario.TipoUsuario tipo, Usuario.EstadoUsuario estado) {
        return usuarioRepository.buscarPorFiltros(tipo, estado).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, Usuario.EstadoUsuario estado, String emailAdministrador) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getEmail().equalsIgnoreCase(emailAdministrador)
                && estado == Usuario.EstadoUsuario.INACTIVO) {
            throw new IllegalStateException("No puedes inactivar tu propia cuenta");
        }
        usuario.setEstado(estado);
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getTipoUsuario(),
                usuario.getEstado(),
                usuario.getFechaRegistro());
    }
}
