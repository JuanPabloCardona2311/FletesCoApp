package com.example.mssolicitudes.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTokenServicioTest {

    // Claves ficticias, solo para pruebas: no son secretos del proyecto.
    static final String CLAVE_PRUEBA = "clave-de-prueba-solo-para-tests-0123456789";
    private static final String OTRA_CLAVE = "otra-clave-de-prueba-distinta-9876543210xx";

    private JwtService jwtService;

    @BeforeEach
    void preparar() {
        jwtService = jwtServiceDePrueba();
    }

    @Test
    void aceptaUnTokenDeServicioValido() {
        assertThat(jwtService.validarTokenServicio(tokenServicio("ms-admin-reportes", CLAVE_PRUEBA, 60_000)))
                .contains("ms-admin-reportes");
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        assertThat(jwtService.validarTokenServicio(tokenServicio("ms-admin-reportes", OTRA_CLAVE, 60_000))).isEmpty();
    }

    @Test
    void rechazaUnServicioFueraDeLaLista() {
        assertThat(jwtService.validarTokenServicio(tokenServicio("ms-desconocido", CLAVE_PRUEBA, 60_000))).isEmpty();
    }

    @Test
    void rechazaUnTokenVencidoOVacio() {
        assertThat(jwtService.validarTokenServicio(tokenServicio("ms-admin-reportes", CLAVE_PRUEBA, -1_000))).isEmpty();
        assertThat(jwtService.validarTokenServicio(null)).isEmpty();
        assertThat(jwtService.validarTokenServicio("no-es-un-jwt")).isEmpty();
    }

    static JwtService jwtServiceDePrueba() {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "secretKey", CLAVE_PRUEBA);
        ReflectionTestUtils.setField(servicio, "jwtExpiration", 60_000L);
        return servicio;
    }

    static String tokenServicio(String servicio, String clave, long vigenciaMs) {
        return Jwts.builder()
                .subject(servicio)
                .claim("tipo", "SERVICIO")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + vigenciaMs))
                .signWith(Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
