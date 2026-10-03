package com.example.msadmin.controller;

import com.example.msadmin.dto.CambiarEstadoUsuarioRequest;
import com.example.msadmin.dto.UsuarioResponse;
import com.example.msadmin.entity.Usuario;
import com.example.msadmin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @RequestParam(required = false) Usuario.TipoUsuario tipo,
            @RequestParam(required = false) Usuario.EstadoUsuario estado) {
        return ResponseEntity.ok(adminService.listarUsuarios(tipo, estado));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.cambiarEstado(id, request.estado(), authentication.getName()));
    }
}
