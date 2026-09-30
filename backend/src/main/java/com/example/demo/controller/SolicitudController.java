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
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/aceptada")
    @PreAuthorize("hasRole('DESPACHADOR') or hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> obtenerSolicitudAceptadaActual() {
        return ResponseEntity.ok(solicitudService.obtenerSolicitudAceptadaActual());
    }

    /**
     * Fletes del despachador en seguimiento: publicados, en proceso o pendientes de confirmar recepción.
     */
    @GetMapping("/mis-fletes")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<List<SolicitudDetalleResponse>> listarFletesDespachador() {
        return ResponseEntity.ok(solicitudService.listarFletesDespachador());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DESPACHADOR') or hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> obtenerSolicitudPorId(
            @PathVariable Long id) {
        SolicitudDetalleResponse response = solicitudService.obtenerSolicitudPorId(id);

        return ResponseEntity.ok(response);
    }

    /**
     * El conductor inicia el viaje tras recoger la carga: ACEPTADA → EN_CURSO.
     */
    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> iniciarViaje(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.iniciarViaje(id));
    }

    /**
     * El conductor marca la carga como entregada: EN_CURSO → COMPLETADA.
     */
    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<SolicitudDetalleResponse> marcarEntregada(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.marcarEntregada(id));
    }

    /**
     * El despachador confirma la recepción y se libera el pago al conductor: RETENIDO → LIBERADO.
     */
    @PatchMapping("/{id}/confirmar-entrega")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<SolicitudDetalleResponse> confirmarEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.confirmarEntrega(id));
    }
}
