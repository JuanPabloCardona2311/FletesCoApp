package com.example.msperfil.controller;

import com.example.msperfil.dto.response.DocumentoResponse;
import com.example.msperfil.service.DocumentoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** E2-02 / E2-03: documentos del conductor autenticado (licencia, SOAT, tecnomecánica...). */
@RestController
@RequestMapping("/api/perfiles/conductor/documentos")
public class DocumentoController {
    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<DocumentoResponse> subir(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam(value = "fechaVencimiento", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
            @RequestParam(value = "vehiculoId", required = false) Long vehiculoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.subir(archivo, tipoDocumento, fechaVencimiento, vehiculoId));
    }

    @GetMapping
    @PreAuthorize("hasRole('CONDUCTOR')")
    public ResponseEntity<List<DocumentoResponse>> listar() {
        return ResponseEntity.ok(documentoService.listar());
    }
}
