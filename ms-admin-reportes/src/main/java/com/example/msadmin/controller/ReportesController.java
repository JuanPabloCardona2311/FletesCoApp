package com.example.msadmin.controller;

import com.example.msadmin.dto.ResumenReportesResponse;
import com.example.msadmin.service.ReportesService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenReportesResponse> resumen() {
        return ResponseEntity.ok(reportesService.resumen());
    }

    @GetMapping("/solicitudes/exportar")
    public ResponseEntity<byte[]> exportarSolicitudes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("reporte-solicitudes.csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(reportesService.exportarSolicitudes());
    }
}
