package com.example.msadmin.service.impl;

import com.example.msadmin.dto.ResumenReportesResponse;
import com.example.msadmin.entity.Disputa;
import com.example.msadmin.entity.Solicitud;
import com.example.msadmin.entity.Usuario;
import com.example.msadmin.repository.DisputaRepository;
import com.example.msadmin.repository.SolicitudRepository;
import com.example.msadmin.repository.UsuarioRepository;
import com.example.msadmin.service.ReportesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportesServiceImpl implements ReportesService {

    private final SolicitudRepository solicitudRepository;
    private final DisputaRepository disputaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReportesServiceImpl(
            SolicitudRepository solicitudRepository,
            DisputaRepository disputaRepository,
            UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.disputaRepository = disputaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenReportesResponse resumen() {
        Map<String, Long> solicitudes = withAllValues(
                Arrays.stream(Solicitud.EstadoSolicitud.values()).map(Enum::name).toList(),
                solicitudRepository.contarPorEstado());
        Map<String, Long> disputas = withAllValues(
                Arrays.stream(Disputa.EstadoDisputa.values()).map(Enum::name).toList(),
                disputaRepository.contarPorEstado());
        Map<String, Long> usuariosEstado = withAllValues(
                Arrays.stream(Usuario.EstadoUsuario.values()).map(Enum::name).toList(),
                usuarioRepository.contarPorEstado());
        Map<String, Long> usuariosTipo = withAllValues(
                Arrays.stream(Usuario.TipoUsuario.values()).map(Enum::name).toList(),
                usuarioRepository.contarPorTipo());

        long totalSolicitudes = solicitudes.values().stream().mapToLong(Long::longValue).sum();
        return new ResumenReportesResponse(solicitudes, disputas, usuariosEstado, usuariosTipo, totalSolicitudes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarSolicitudes() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,origen,destino,estado,precioOfrecido,fechaPublicacion,fechaRecogida,fechaEntregaEstimada\n");
        solicitudRepository.findAll().forEach(solicitud -> csv.append(row(
                solicitud.getId(),
                solicitud.getOrigen(),
                solicitud.getDestino(),
                solicitud.getEstado(),
                solicitud.getPrecioOfrecido(),
                solicitud.getFechaPublicacion(),
                solicitud.getFechaRecogida(),
                solicitud.getFechaEntregaEstimada())));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Long> withAllValues(List<String> values, List<Object[]> groupedRows) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach(value -> result.put(value, 0L));
        groupedRows.forEach(row -> result.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue()));
        return result;
    }

    private String row(Object... values) {
        return Arrays.stream(values)
                .map(value -> escape(value == null ? "" : value.toString()))
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "\n";
    }

    private String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
