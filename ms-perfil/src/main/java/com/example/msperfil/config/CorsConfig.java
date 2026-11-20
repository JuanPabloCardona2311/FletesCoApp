package com.example.msperfil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final List<String> origenes;

    // Orígenes permitidos desde app.cors.origenes (variable APP_CORS_ORIGENES), separados
    // por comas. Sin definirla quedan los de desarrollo local. En el servidor va la URL
    // pública con https: detrás del túnel el navegador usa un origen distinto al que ve Spring.
    public CorsConfig(@Value("${app.cors.origenes:http://localhost:5173,http://127.0.0.1:5173}") String[] origenes) {
        this.origenes = Arrays.stream(origenes)
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origenes);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
