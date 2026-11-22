package com.example.mssolicitudes.evento;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

// Envía cada EventoSolicitud a los servicios conectados al canal interno.
@Component
public class DifusorEventos {

    private static final Logger log = LoggerFactory.getLogger(DifusorEventos.class);

    private final CanalServiciosHandler canal;
    private final ObjectMapper objectMapper;

    public DifusorEventos(CanalServiciosHandler canal, ObjectMapper objectMapper) {
        this.canal = canal;
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
    }
}
