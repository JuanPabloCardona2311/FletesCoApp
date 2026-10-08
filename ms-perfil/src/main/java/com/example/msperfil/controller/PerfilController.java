package com.example.msperfil.controller;

import com.example.msperfil.dto.request.*;
import com.example.msperfil.dto.response.*;
import com.example.msperfil.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody PerfilConductorRequest request) {
        return ResponseEntity.ok(perfilService.guardarPerfilConductor(request));
    }

    @PatchMapping("/conductor/vehiculos/{vehiculoId}/activar")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<PerfilConductorResponse> activarVehiculo(@PathVariable Long vehiculoId) {
        return ResponseEntity.ok(perfilService.activarVehiculo(vehiculoId));
    }

    @PutMapping("/conductor/vehiculos/{vehiculoId}/activar")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<PerfilConductorResponse> activarVehiculoPut(@PathVariable Long vehiculoId) {
        return ResponseEntity.ok(perfilService.activarVehiculo(vehiculoId));
    }

    @PutMapping("/conductor/ubicacion")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<Void> actualizarUbicacionConductor(
            @Valid @RequestBody UbicacionConductorRequest request) {
        perfilService.actualizarUbicacionConductor(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/despachador")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<PerfilDespachadorResponse> obtenerPerfilDespachador() {
        return ResponseEntity.ok(perfilService.obtenerPerfilDespachador());
    }

    @PutMapping("/despachador")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<PerfilDespachadorResponse> guardarPerfilDespachador(
            @Valid @RequestBody PerfilDespachadorRequest request) {
        return ResponseEntity.ok(perfilService.guardarPerfilDespachador(request));
    }

    @GetMapping("/datos")
    @PreAuthorize("hasAnyRole('CONDUCTOR','DESPACHADOR')")
    public ResponseEntity<DatosPersonalesResponse> obtenerDatosPersonales() {
        return ResponseEntity.ok(perfilService.obtenerDatosPersonales());
    }

    @PutMapping("/datos")
    @PreAuthorize("hasAnyRole('CONDUCTOR','DESPACHADOR')")
    public ResponseEntity<DatosPersonalesResponse> actualizarDatosPersonales(
            @Valid @RequestBody DatosPersonalesRequest request) {
        return ResponseEntity.ok(perfilService.actualizarDatosPersonales(request));
    }
}
