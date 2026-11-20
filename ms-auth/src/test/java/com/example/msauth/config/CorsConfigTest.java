package com.example.msauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void permiteSoloLosOrigenesConfigurados() {
        CorsConfig corsConfig = new CorsConfig(new String[] {"http://localhost:5173", "  https://fletesco.ejemplo.org  ", ""});
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        CorsConfiguration configuration = corsConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://fletesco.ejemplo.org");
        assertThat(configuration.checkOrigin("https://fletesco.ejemplo.org")).isEqualTo("https://fletesco.ejemplo.org");
        assertThat(configuration.checkOrigin("https://otro.org")).isNull();
        assertThat(configuration.getAllowedMethods()).contains("PATCH");
    }
}
