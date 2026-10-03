package com.example.msadmin.controller;

import com.example.msadmin.dto.DisputaResponse;
import com.example.msadmin.dto.ResolverDisputaRequest;
import com.example.msadmin.entity.Disputa;
import com.example.msadmin.service.DisputaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/disputas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class DisputaController {

    private final DisputaService disputaService;

    public DisputaController(DisputaService disputaService) {
        this.disputaService = disputaService;
    }

    @GetMapping
    public ResponseEntity<List<DisputaResponse>> listar(
            @RequestParam(required = false) Disputa.EstadoDisputa estado) {
        return ResponseEntity.ok(disputaService.listar(estado));
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<DisputaResponse> resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolverDisputaRequest request) {
        return ResponseEntity.ok(disputaService.resolver(id, request.resolucion()));
    }
}
