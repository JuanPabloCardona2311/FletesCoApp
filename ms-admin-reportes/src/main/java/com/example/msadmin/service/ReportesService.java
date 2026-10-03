package com.example.msadmin.service;

import com.example.msadmin.dto.ResumenReportesResponse;

public interface ReportesService {

    ResumenReportesResponse resumen();

    byte[] exportarSolicitudes();
}
