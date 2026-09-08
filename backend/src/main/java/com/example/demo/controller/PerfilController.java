package com.example.demo.controller;

import com.example.demo.dto.request.PerfilConductorRequest;
import com.example.demo.dto.request.PerfilDespachadorRequest;
import com.example.demo.dto.response.PerfilConductorResponse;
import com.example.demo.dto.response.PerfilDespachadorResponse;
import com.example.demo.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfiles")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/conductor")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<PerfilConductorResponse> obtenerPerfilConductor() {
        return ResponseEntity.ok(perfilService.obtenerPerfilConductor());
    }

    @PutMapping("/conductor")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<PerfilConductorResponse> guardarPerfilConductor(
            @Valid @RequestBody PerfilConductorRequest request
    ) {
        return ResponseEntity.ok(perfilService.guardarPerfilConductor(request));
    }

    @GetMapping("/despachador")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<PerfilDespachadorResponse> obtenerPerfilDespachador() {
        return ResponseEntity.ok(perfilService.obtenerPerfilDespachador());
    }

    @PutMapping("/despachador")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<PerfilDespachadorResponse> guardarPerfilDespachador(
            @Valid @RequestBody PerfilDespachadorRequest request
    ) {
        return ResponseEntity.ok(perfilService.guardarPerfilDespachador(request));
    }
}
