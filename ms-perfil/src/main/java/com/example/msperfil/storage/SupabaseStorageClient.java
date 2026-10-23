package com.example.msperfil.storage;

import com.example.msperfil.exception.AlmacenamientoException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente mínimo de la API REST de Supabase Storage (bucket privado).
 * Usa la clave secreta del proyecto: solo vive en el backend y nunca se registra en el log.
 */
@Component
public class SupabaseStorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

    private final String urlProyecto;
    private final String clave;
    private final String bucket;
    private final Duration validezEnlace;
    private final RestClient restClient;

    public SupabaseStorageClient(
            @Value("${supabase.url:}") String urlProyecto,
            @Value("${supabase.service-role-key:}") String clave,
            @Value("${supabase.storage.bucket:documentos}") String bucket,
            @Value("${supabase.storage.minutos-enlace:5}") long minutosEnlace) {
        this.urlProyecto = normalizarUrl(urlProyecto);
        this.clave = clave.strip();
        this.bucket = bucket;
        this.validezEnlace = Duration.ofMinutes(minutosEnlace);

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(this.urlProyecto + "/storage/v1")
                .defaultHeaders(this::agregarAutenticacion)
                .build();

        if (!estaConfigurado()) {
            log.warn("Supabase Storage no está configurado (faltan SUPABASE_URL o SUPABASE_SERVICE_ROLE_KEY): "
                    + "la subida de documentos responderá 503 hasta configurarlo.");
        }
    }

    /**
     * Deja solo la URL base del proyecto (https://ref.supabase.co): quita espacios,
     * barras finales y un sufijo /rest/v1 o /storage/v1 pegado por error desde el panel.
     */
    public static String normalizarUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalizada = url.strip().replaceAll("/+$", "");
        normalizada = normalizada.replaceFirst("(?i)/(rest|storage)/v1$", "");
        return normalizada.replaceAll("/+$", "");
    }

    public boolean estaConfigurado() {
        return !urlProyecto.isEmpty() && !clave.isEmpty();
    }

    public Duration getValidezEnlace() {
        return validezEnlace;
    }

    public void subir(String ruta, byte[] contenido, String contentType) {
        exigirConfiguracion();
        try {
            restClient.post()
                    .uri("/object/" + bucket + "/" + ruta)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "false")
                    .body(contenido)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw fallo("subir", ruta, e, "No fue posible guardar el archivo. Inténtalo de nuevo.");
        }
    }

    /** Borrado de mejor esfuerzo: un archivo huérfano no debe hacer fallar la operación del usuario. */
    public void eliminarSilencioso(String ruta) {
        if (!estaConfigurado() || ruta == null || ruta.isBlank()) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/object/" + bucket + "/" + ruta)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo borrar el archivo anterior {} del bucket {}: {}", ruta, bucket, describir(e));
        }
    }

    /** Enlace temporal para ver un archivo del bucket privado. */
    public EnlaceFirmado firmar(String ruta) {
        exigirConfiguracion();
        try {
            Map<?, ?> respuesta = restClient.post()
                    .uri("/object/sign/" + bucket + "/" + ruta)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", validezEnlace.toSeconds()))
                    .retrieve()
                    .body(Map.class);
            Object rutaFirmada = respuesta == null ? null : respuesta.get("signedURL");
            if (rutaFirmada == null) {
                throw new AlmacenamientoException("No fue posible generar el enlace del archivo.");
            }
            return new EnlaceFirmado(urlProyecto + "/storage/v1" + rutaFirmada, Instant.now().plus(validezEnlace));
        } catch (RestClientException e) {
            throw fallo("firmar", ruta, e, "No fue posible generar el enlace del archivo.");
        }
    }

    public record EnlaceFirmado(String url, Instant expiraEn) {
    }

    // Las claves nuevas (sb_secret_...) van solo en el header apikey; las legacy
    // (JWT service_role) además en Authorization, como espera Storage.
    private void agregarAutenticacion(HttpHeaders headers) {
        if (clave.isEmpty()) {
            return;
        }
        headers.set("apikey", clave);
        if (clave.startsWith("eyJ")) {
            headers.setBearerAuth(clave);
        }
    }

    private void exigirConfiguracion() {
        if (!estaConfigurado()) {
            throw new AlmacenamientoException(
                    "El almacenamiento de documentos no está configurado en el servidor. Avisa al administrador.");
        }
    }

    private AlmacenamientoException fallo(String operacion, String ruta, RestClientException e, String mensaje) {
        log.error("Supabase Storage falló al {} {}: {}", operacion, ruta, describir(e));
        return new AlmacenamientoException(mensaje, e);
    }

    private static String describir(RestClientException e) {
        if (e instanceof RestClientResponseException r) {
            return r.getStatusCode() + " " + r.getResponseBodyAsString();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
