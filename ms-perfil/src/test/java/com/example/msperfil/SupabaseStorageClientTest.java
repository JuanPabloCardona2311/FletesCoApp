package com.example.msperfil;

import com.example.msperfil.storage.SupabaseStorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseStorageClientTest {

    private static final String BASE = "https://abcdefgh.supabase.co";

    @ParameterizedTest
    @ValueSource(strings = {
            "https://abcdefgh.supabase.co",
            "  https://abcdefgh.supabase.co  ",
            "https://abcdefgh.supabase.co/",
            "https://abcdefgh.supabase.co///",
            "https://abcdefgh.supabase.co/rest/v1",
            "https://abcdefgh.supabase.co/rest/v1/",
            "https://abcdefgh.supabase.co/storage/v1",
            " https://abcdefgh.supabase.co/storage/v1/ ",
            "https://abcdefgh.supabase.co/REST/V1"
    })
    void normalizaLaUrlDelProyecto(String url) {
        assertThat(SupabaseStorageClient.normalizarUrl(url)).isEqualTo(BASE);
    }

    @Test
    void urlVaciaONulaQuedaVacia() {
        assertThat(SupabaseStorageClient.normalizarUrl(null)).isEmpty();
        assertThat(SupabaseStorageClient.normalizarUrl("   ")).isEmpty();
    }

    @Test
    void soloQuitaElSufijoAlFinal() {
        // Un /rest/v1 en medio de la ruta no es el sufijo del panel y se conserva.
        assertThat(SupabaseStorageClient.normalizarUrl("https://x.supabase.co/rest/v1/otra"))
                .isEqualTo("https://x.supabase.co/rest/v1/otra");
    }

    @Test
    void urlSoloConEspaciosNoCuentaComoConfigurada() {
        SupabaseStorageClient cliente = new SupabaseStorageClient("   ", "sb_secret_prueba", "documentos", 5);
        assertThat(cliente.estaConfigurado()).isFalse();
    }
}
