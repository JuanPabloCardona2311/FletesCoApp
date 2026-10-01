package com.example.msperfil.service;

import com.example.msperfil.dto.request.*;
import com.example.msperfil.dto.response.*;

public interface PerfilService {
    PerfilConductorResponse obtenerPerfilConductor();
    PerfilConductorResponse guardarPerfilConductor(PerfilConductorRequest request);
    PerfilConductorResponse activarVehiculo(Long vehiculoId);
    void actualizarUbicacionConductor(UbicacionConductorRequest request);
    PerfilDespachadorResponse obtenerPerfilDespachador();
    PerfilDespachadorResponse guardarPerfilDespachador(PerfilDespachadorRequest request);
}
