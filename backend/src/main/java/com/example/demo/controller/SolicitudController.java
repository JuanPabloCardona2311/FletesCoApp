package com.example.demo.controller;

import com.example.demo.dto.request.AceptarSolicitudRequest;
import com.example.demo.dto.request.PublicarSolicitudRequest;
import com.example.demo.dto.response.SolicitudAceptadaResponse;
import com.example.demo.dto.response.SolicitudPublicadaResponse;
import com.example.demo.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.dto.response.SolicitudDetalleResponse;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /**
     * E4-01: Publicar una solicitud de flete como despachador.
     */
    @PostMapping
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<SolicitudPublicadaResponse> publicarSolicitud(
            @Valid @RequestBody PublicarSolicitudRequest request) {
        SolicitudPublicadaResponse response = solicitudService.publicarSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Listar solicitudes disponibles compatibles con el vehículo del conductor.
     */
    @GetMapping("/disponibles")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<List<SolicitudPublicadaResponse>> listarDisponibles() {
        List<SolicitudPublicadaResponse> response = solicitudService.listarSolicitudesDisponibles();
        return ResponseEntity.ok(response);
    }

    /**
     * Aceptar una solicitud publicada como conductor.
     */
    @PostMapping("/aceptar")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudAceptadaResponse> aceptarSolicitud(
            @Valid @RequestBody AceptarSolicitudRequest request) {
        SolicitudAceptadaResponse response = solicitudService.aceptarSolicitud(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<SolicitudDetalleResponse> obtenerSolicitudPorId(
            @PathVariable Long id) {
        SolicitudDetalleResponse response = solicitudService.obtenerSolicitudPorId(id);

        return ResponseEntity.ok(response);
    }
}
