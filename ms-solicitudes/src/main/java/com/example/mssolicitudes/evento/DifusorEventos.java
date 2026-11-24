package com.example.mssolicitudes.evento;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import com.example.mssolicitudes.websocket.CanalUsuariosHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

// Envía cada EventoSolicitud a los servicios conectados al canal interno y avisa a los
// navegadores de conductores y despachadores que corresponda.
@Component
public class DifusorEventos {

    private static final Logger log = LoggerFactory.getLogger(DifusorEventos.class);

    private final CanalServiciosHandler canal;
    private final CanalUsuariosHandler canalUsuarios;
    private final ObjectMapper objectMapper;

    public DifusorEventos(CanalServiciosHandler canal, CanalUsuariosHandler canalUsuarios, ObjectMapper objectMapper) {
        this.canal = canal;
        this.canalUsuarios = canalUsuarios;
        this.objectMapper = objectMapper;
    }

    // AFTER_COMMIT: solo se anuncia lo que quedó guardado; si la transacción hace
    // rollback, el evento se descarta. Nunca lanza excepción: un fallo del WebSocket
    // no puede afectar la petición HTTP que originó el cambio.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void difundir(EventoSolicitud evento) {
        try {
            canal.difundir(objectMapper.writeValueAsString(evento));
        } catch (Exception ex) {
            log.warn("No se pudo difundir el evento {} de la solicitud {}: {}",
                    evento.tipo(), evento.solicitudId(), ex.getMessage());
        }
        try {
            avisarNavegadores(evento);
        } catch (Exception ex) {
            log.warn("No se pudo avisar a los navegadores del evento {} de la solicitud {}: {}",
                    evento.tipo(), evento.solicitudId(), ex.getMessage());
        }
    }

    // El navegador recibe un aviso mínimo (tipo, solicitudId y estado), sin ids de otras
    // personas ni datos personales: los datos los pide por REST, que valida permisos.
    private void avisarNavegadores(EventoSolicitud evento) {
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("tipo", evento.tipo());
        aviso.put("solicitudId", evento.solicitudId());
        aviso.put("estado", evento.estadoNuevo());
        String json = objectMapper.writeValueAsString(aviso);

        switch (evento.tipo()) {
            // Cada conductor recarga /disponibles, que ya filtra por su vehículo.
            case EventoSolicitud.SOLICITUD_CREADA -> canalUsuarios.enviarATodosLosConductores(json);
            // A los demás conductores para que desaparezca de su lista, y al despachador dueño.
            case EventoSolicitud.SOLICITUD_ACEPTADA -> {
                canalUsuarios.enviarATodosLosConductores(json);
                canalUsuarios.enviarADespachador(evento.despachadorId(), json);
            }
            case EventoSolicitud.VIAJE_INICIADO, EventoSolicitud.ENTREGA_MARCADA, EventoSolicitud.PAGO_LIBERADO -> {
                canalUsuarios.enviarADespachador(evento.despachadorId(), json);
                canalUsuarios.enviarAConductor(evento.conductorId(), json);
            }
            default -> log.debug("Evento {} sin destino en los navegadores", evento.tipo());
        }
    }
}
