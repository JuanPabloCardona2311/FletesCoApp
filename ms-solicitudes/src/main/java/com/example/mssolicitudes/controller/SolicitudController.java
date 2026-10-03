package com.example.mssolicitudes.controller;

import com.example.mssolicitudes.dto.request.*;
import com.example.mssolicitudes.dto.response.*;
import com.example.mssolicitudes.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<SolicitudPublicadaResponse> publicarSolicitud(@Valid @RequestBody PublicarSolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.publicarSolicitud(request));
    }

    @GetMapping("/disponibles") @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<List<SolicitudPublicadaResponse>> listarDisponibles() {
        return ResponseEntity.ok(solicitudService.listarSolicitudesDisponibles());
    }

    @PostMapping("/aceptar") @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudAceptadaResponse> aceptarSolicitud(
            @Valid @RequestBody AceptarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.aceptarSolicitud(request));
    }

    @GetMapping("/aceptada") @PreAuthorize("hasRole('DESPACHADOR') or hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> obtenerSolicitudAceptadaActual() {
        return ResponseEntity.ok(solicitudService.obtenerSolicitudAceptadaActual());
    }

    @GetMapping("/mis-fletes") @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<List<SolicitudDetalleResponse>> listarFletesDespachador() {
        return ResponseEntity.ok(solicitudService.listarFletesDespachador());
    }

    @GetMapping("/{id}") @PreAuthorize("hasRole('DESPACHADOR') or hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> obtenerSolicitudPorId(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerSolicitudPorId(id));
    }

    @PatchMapping("/{id}/iniciar") @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> iniciarViaje(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.iniciarViaje(id));
    }

    @PatchMapping("/{id}/entregar") @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> marcarEntregada(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.marcarEntregada(id));
    }

    @PatchMapping("/{id}/confirmar-entrega") @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<SolicitudDetalleResponse> confirmarEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.confirmarEntrega(id));
    }
}
