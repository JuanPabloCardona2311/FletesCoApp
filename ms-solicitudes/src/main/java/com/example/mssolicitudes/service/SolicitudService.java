package com.example.mssolicitudes.service;

import com.example.mssolicitudes.dto.request.*;
import com.example.mssolicitudes.dto.response.*;
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
