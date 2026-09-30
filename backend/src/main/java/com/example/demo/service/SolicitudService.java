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
    SolicitudDetalleResponse obtenerSolicitudAceptadaActual();
    List<SolicitudDetalleResponse> listarFletesDespachador();
    SolicitudDetalleResponse iniciarViaje(Long id);
    SolicitudDetalleResponse marcarEntregada(Long id);
    SolicitudDetalleResponse confirmarEntrega(Long id);
}
