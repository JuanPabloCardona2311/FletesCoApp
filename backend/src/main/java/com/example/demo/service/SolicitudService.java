package com.example.demo.service;

import com.example.demo.dto.request.AceptarSolicitudRequest;
import com.example.demo.dto.request.PublicarSolicitudRequest;
import com.example.demo.dto.response.SolicitudAceptadaResponse;
import com.example.demo.dto.response.SolicitudDetalleResponse;
import com.example.demo.dto.response.SolicitudPublicadaResponse;

import java.util.List;

public interface SolicitudService {
    SolicitudPublicadaResponse publicarSolicitud(PublicarSolicitudRequest request);
    List<SolicitudPublicadaResponse> listarSolicitudesDisponibles();
    SolicitudAceptadaResponse aceptarSolicitud(AceptarSolicitudRequest request);
    SolicitudDetalleResponse obtenerSolicitudPorId(Long id);
}
