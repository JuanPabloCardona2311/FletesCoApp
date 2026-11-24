package com.example.msperfil.evento;

import com.example.msperfil.websocket.PublicadorUbicacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Publica la ubicación por WebSocket solo después del commit: si la transacción hace
// rollback, no se anuncia. Nunca lanza excepción, así un fallo del WebSocket no puede
// hacer fallar el PUT del conductor; la ubicación ya quedó guardada en la base de datos.
@Component
public class OyenteUbicacion {

    private static final Logger log = LoggerFactory.getLogger(OyenteUbicacion.class);

    private final PublicadorUbicacion publicador;

    public OyenteUbicacion(PublicadorUbicacion publicador) {
        this.publicador = publicador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alGuardarUbicacion(UbicacionActualizada evento) {
        try {
            publicador.publicar(evento.conductorId(), evento.latitud(), evento.longitud(), evento.fecha());
        } catch (Exception ex) {
            log.debug("No se pudo publicar la ubicación del conductor {}: {}", evento.conductorId(), ex.getMessage());
        }
    }
}
