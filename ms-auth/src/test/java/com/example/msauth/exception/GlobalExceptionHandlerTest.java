package com.example.msauth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void usuarioDeshabilitadoResponde403ConMensajeClaro() {
        ResponseEntity<Map<String, Object>> respuesta =
                new GlobalExceptionHandler().handleDisabledException(new DisabledException("User is disabled"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody())
                .containsEntry("status", 403)
                .containsEntry("error", "Tu cuenta está inactiva. Contacta al administrador.")
                .containsKey("timestamp");
    }
}
