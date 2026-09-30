package com.example.demo.service;

import com.example.demo.dto.request.PerfilConductorRequest;
import com.example.demo.dto.request.PerfilDespachadorRequest;
import com.example.demo.dto.response.PerfilConductorResponse;
import com.example.demo.dto.response.PerfilDespachadorResponse;

public interface PerfilService {
    PerfilConductorResponse obtenerPerfilConductor();
    PerfilConductorResponse guardarPerfilConductor(PerfilConductorRequest request);
    PerfilConductorResponse activarVehiculo(Long vehiculoId);
    PerfilDespachadorResponse obtenerPerfilDespachador();
    PerfilDespachadorResponse guardarPerfilDespachador(PerfilDespachadorRequest request);
}
