package com.example.msauth.service;

import com.example.msauth.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Prototipo sin servidor de correo: en lugar de enviar un email, deja el enlace
 * en el log de ms-auth para poder copiarlo (docker compose logs ms-auth).
 */
@Component
@ConditionalOnProperty(name = "app.recuperacion.simulada", havingValue = "true", matchIfMissing = true)
public class EnvioEnlaceRecuperacionSimulado implements EnvioEnlaceRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(EnvioEnlaceRecuperacionSimulado.class);

    @Override
    public void enviar(Usuario usuario, String enlace) {
        log.info("[CORREO SIMULADO - no se envía ningún email] Recuperación de contraseña para {}: {}",
                usuario.getEmail(), enlace);
    }
}
