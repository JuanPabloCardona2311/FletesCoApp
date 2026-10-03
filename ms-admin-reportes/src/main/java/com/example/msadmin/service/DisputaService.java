package com.example.msadmin.service;

import com.example.msadmin.dto.DisputaResponse;
import com.example.msadmin.entity.Disputa;

import java.util.List;

public interface DisputaService {

    List<DisputaResponse> listar(Disputa.EstadoDisputa estado);

    DisputaResponse resolver(Long id, String resolucion);
}
