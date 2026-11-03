package com.example.msperfil;

import com.example.msperfil.dto.response.DocumentoResponse;
import com.example.msperfil.entity.Conductor;
import com.example.msperfil.entity.DocumentoConductor;
import com.example.msperfil.entity.DocumentoVehiculo;
import com.example.msperfil.entity.Usuario;
import com.example.msperfil.entity.Vehiculo;
import com.example.msperfil.exception.DocumentoInvalidoException;
import com.example.msperfil.repository.ConductorRepository;
import com.example.msperfil.repository.DocumentoConductorRepository;
import com.example.msperfil.repository.DocumentoVehiculoRepository;
import com.example.msperfil.repository.UsuarioRepository;
import com.example.msperfil.repository.VehiculoRepository;
import com.example.msperfil.service.DocumentoService;
import com.example.msperfil.storage.SupabaseStorageClient;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lógica de documentos sin base de datos ni Supabase: repositorios y Storage simulados. */
class DocumentoServiceTest {

    private static final byte[] PDF = "%PDF-1.7\n contenido".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    private static final LocalDate FUTURA = LocalDate.now().plusYears(1);

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ConductorRepository conductorRepository = mock(ConductorRepository.class);
    private final VehiculoRepository vehiculoRepository = mock(VehiculoRepository.class);
    private final DocumentoConductorRepository docConductorRepo = mock(DocumentoConductorRepository.class);
    private final DocumentoVehiculoRepository docVehiculoRepo = mock(DocumentoVehiculoRepository.class);
    private final SupabaseStorageClient storage = mock(SupabaseStorageClient.class);

    private DocumentoService servicio;
    private Conductor conductor;
    private Vehiculo vehiculoPropio;

    @BeforeEach
    void preparar() {
        servicio = new DocumentoService(usuarioRepository, conductorRepository, vehiculoRepository,
                docConductorRepo, docVehiculoRepo, storage);

        Usuario usuario = Usuario.builder().id(1L).email("conductor@fletesco.test")
                .tipoUsuario(Usuario.TipoUsuario.CONDUCTOR).build();
        conductor = Conductor.builder().id(10L).usuario(usuario).build();
        vehiculoPropio = Vehiculo.builder().id(100L).conductor(conductor).placa("ABC123").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("conductor@fletesco.test", null, java.util.List.of()));
        when(usuarioRepository.findByEmail("conductor@fletesco.test")).thenReturn(Optional.of(usuario));
        when(conductorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(100L)).thenReturn(Optional.of(vehiculoPropio));
        when(docConductorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(docVehiculoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    @Test
    void subeLicenciaPdfConNombreGeneradoPorElServidor() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "mi licencia ORIGINAL.pdf", "application/pdf", PDF);

        DocumentoResponse respuesta = servicio.subir(archivo, "LICENCIA_CONDUCCION", FUTURA, null);

        ArgumentCaptor<String> ruta = ArgumentCaptor.forClass(String.class);
        verify(storage).subir(ruta.capture(), eq(PDF), eq("application/pdf"));
        assertThat(ruta.getValue()).matches("conductores/10/[0-9a-f-]{36}\\.pdf").doesNotContain("ORIGINAL");
        assertThat(respuesta.getEstadoVerificacion()).isEqualTo("PENDIENTE");
        assertThat(respuesta.getCategoria()).isEqualTo("CONDUCTOR");
    }

    @Test
    void elContentTypeDelClienteNoImporta_mandaElContenidoReal() {
        // Un PNG que el cliente declara como PDF se guarda como PNG.
        MockMultipartFile archivo = new MockMultipartFile("archivo", "soat.pdf", "application/pdf", PNG);

        servicio.subir(archivo, "SOAT", FUTURA, 100L);

        verify(storage).subir(anyString(), eq(PNG), eq("image/png"));
    }

    @Test
    void aceptaJpgParaTecnomecanica() {
        servicio.subir(new MockMultipartFile("archivo", "tecno.jpeg", "image/jpeg", JPG), "TECNOMECANICA", FUTURA, 100L);

        verify(storage).subir(anyString(), eq(JPG), eq("image/jpeg"));
    }

    @Test
    void rechazaArchivoRenombradoQueNoEsPdfJpgNiPng() {
        MockMultipartFile falso = new MockMultipartFile("archivo", "virus.pdf", "application/pdf", "MZ ejecutable".getBytes());

        assertThatThrownBy(() -> servicio.subir(falso, "LICENCIA_CONDUCCION", FUTURA, null))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("Tipo de archivo no permitido");
        verify(storage, never()).subir(anyString(), any(), anyString());
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> servicio.subir(vacio, "LICENCIA_CONDUCCION", FUTURA, null))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void rechazaArchivoDeMasDe5Mb() {
        byte[] grande = new byte[(int) DocumentoService.TAMANO_MAXIMO_BYTES + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        assertThatThrownBy(() -> servicio.subir(new MockMultipartFile("archivo", "g.pdf", "application/pdf", grande),
                "LICENCIA_CONDUCCION", FUTURA, null))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void exigeFechaFuturaParaLicenciaSoatYTecnomecanica() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.pdf", "application/pdf", PDF);

        assertThatThrownBy(() -> servicio.subir(archivo, "SOAT", null, 100L))
                .hasMessageContaining("obligatoria");
        assertThatThrownBy(() -> servicio.subir(archivo, "LICENCIA_CONDUCCION", LocalDate.now(), null))
                .hasMessageContaining("posterior a hoy");
        assertThatThrownBy(() -> servicio.subir(archivo, "TECNOMECANICA", LocalDate.now().minusDays(3), 100L))
                .hasMessageContaining("posterior a hoy");
    }

    @Test
    void rechazaTipoDeDocumentoDesconocido() {
        assertThatThrownBy(() -> servicio.subir(new MockMultipartFile("archivo", "a.pdf", "application/pdf", PDF),
                "PASAPORTE", FUTURA, null))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("Tipo de documento no válido");
    }

    @Test
    void noPermiteSubirDocumentosAlVehiculoDeOtroConductor() {
        Conductor otro = Conductor.builder().id(99L).build();
        when(vehiculoRepository.findById(200L))
                .thenReturn(Optional.of(Vehiculo.builder().id(200L).conductor(otro).build()));

        assertThatThrownBy(() -> servicio.subir(new MockMultipartFile("archivo", "s.pdf", "application/pdf", PDF),
                "SOAT", FUTURA, 200L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(storage, never()).subir(anyString(), any(), anyString());
    }

    @Test
    void reemplazarVuelveAPendienteYBorraElAnteriorSoloTrasConfirmar() {
        DocumentoConductor existente = DocumentoConductor.builder().id(5L).conductor(conductor)
                .tipoDocumento(DocumentoConductor.TipoDocumento.LICENCIA_CONDUCCION)
                .archivoUrl("conductores/10/anterior.pdf")
                .estadoVerificacion(DocumentoConductor.EstadoVerificacion.VERIFICADO).build();
        when(docConductorRepo.findByConductorIdAndTipoDocumento(10L, DocumentoConductor.TipoDocumento.LICENCIA_CONDUCCION))
                .thenReturn(Optional.of(existente));

        servicio.subir(new MockMultipartFile("archivo", "l.png", "image/png", PNG), "LICENCIA_CONDUCCION", FUTURA, null);

        assertThat(existente.getEstadoVerificacion()).isEqualTo(DocumentoConductor.EstadoVerificacion.PENDIENTE);
        assertThat(existente.getArchivoUrl()).isNotEqualTo("conductores/10/anterior.pdf");
        verify(storage, never()).eliminarSilencioso("conductores/10/anterior.pdf");

        // Al confirmar la transacción se borra el archivo reemplazado (y no el nuevo).
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCommit();
            s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        verify(storage).eliminarSilencioso("conductores/10/anterior.pdf");
        verify(storage, never()).eliminarSilencioso(existente.getArchivoUrl());
    }

    @Test
    void siLaBaseFallaSeBorraElArchivoRecienSubido() {
        servicio.subir(new MockMultipartFile("archivo", "s.pdf", "application/pdf", PDF), "SOAT", FUTURA, 100L);
        ArgumentCaptor<DocumentoVehiculo> guardado = ArgumentCaptor.forClass(DocumentoVehiculo.class);
        verify(docVehiculoRepo).save(guardado.capture());

        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verify(storage).eliminarSilencioso(guardado.getValue().getArchivoUrl());
    }
}
