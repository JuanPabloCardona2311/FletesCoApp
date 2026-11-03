package com.example.msperfil.service;

import com.example.msperfil.dto.response.DocumentoResponse;
import com.example.msperfil.entity.Conductor;
import com.example.msperfil.entity.DocumentoConductor;
import com.example.msperfil.entity.DocumentoVehiculo;
import com.example.msperfil.entity.Usuario;
import com.example.msperfil.entity.Vehiculo;
import com.example.msperfil.exception.AlmacenamientoException;
import com.example.msperfil.exception.DocumentoInvalidoException;
import com.example.msperfil.repository.ConductorRepository;
import com.example.msperfil.repository.DocumentoConductorRepository;
import com.example.msperfil.repository.DocumentoVehiculoRepository;
import com.example.msperfil.repository.UsuarioRepository;
import com.example.msperfil.repository.VehiculoRepository;
import com.example.msperfil.storage.SupabaseStorageClient;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * E2-02 / E2-03: documentos del conductor (licencia) y de sus vehículos (SOAT, tecnomecánica...).
 * Subirlos es OPCIONAL: ninguna otra operación de la plataforma depende de ellos.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    // Documentos para los que la fecha de vencimiento es obligatoria y debe ser futura.
    private static final Set<String> TIPOS_CON_VENCIMIENTO = Set.of("LICENCIA_CONDUCCION", "SOAT", "TECNOMECANICA");

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final DocumentoConductorRepository documentoConductorRepository;
    private final DocumentoVehiculoRepository documentoVehiculoRepository;
    private final SupabaseStorageClient storage;

    public DocumentoService(UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            VehiculoRepository vehiculoRepository,
            DocumentoConductorRepository documentoConductorRepository,
            DocumentoVehiculoRepository documentoVehiculoRepository,
            SupabaseStorageClient storage) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.documentoConductorRepository = documentoConductorRepository;
        this.documentoVehiculoRepository = documentoVehiculoRepository;
        this.storage = storage;
    }

    @Transactional
    public DocumentoResponse subir(MultipartFile archivo, String tipoDocumento, LocalDate fechaVencimiento, Long vehiculoId) {
        Conductor conductor = obtenerConductorAutenticado();
        String tipo = tipoDocumento == null ? "" : tipoDocumento.strip().toUpperCase();
        validarFecha(tipo, fechaVencimiento);
        TipoArchivo tipoArchivo = validarArchivo(archivo);
        byte[] contenido = leer(archivo);

        if (esTipoConductor(tipo)) {
            return subirDocumentoConductor(conductor, contenido, tipoArchivo, fechaVencimiento);
        }
        DocumentoVehiculo.TipoDocumento tipoVehiculo = convertirTipoVehiculo(tipo);
        Vehiculo vehiculo = obtenerVehiculoPropio(conductor, vehiculoId);
        return subirDocumentoVehiculo(vehiculo, tipoVehiculo, contenido, tipoArchivo, fechaVencimiento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar() {
        Conductor conductor = obtenerConductorAutenticado();
        List<DocumentoResponse> documentos = new ArrayList<>();
        documentoConductorRepository.findByConductorId(conductor.getId())
                .forEach(doc -> documentos.add(construir(doc, true)));
        documentoVehiculoRepository.findByVehiculo_Conductor_Id(conductor.getId())
                .stream()
                .sorted(Comparator.comparing((DocumentoVehiculo doc) -> doc.getVehiculo().getId()))
                .forEach(doc -> documentos.add(construir(doc, true)));
        return documentos;
    }

    // ── Subida ──────────────────────────────────────────────────────────────

    private DocumentoResponse subirDocumentoConductor(Conductor conductor, byte[] contenido,
            TipoArchivo tipoArchivo, LocalDate fechaVencimiento) {
        DocumentoConductor.TipoDocumento tipo = DocumentoConductor.TipoDocumento.LICENCIA_CONDUCCION;
        String ruta = "conductores/" + conductor.getId() + "/" + UUID.randomUUID() + tipoArchivo.extension;
        storage.subir(ruta, contenido, tipoArchivo.contentType);
        borrarSiLaTransaccionFalla(ruta);

        DocumentoConductor documento = documentoConductorRepository
                .findByConductorIdAndTipoDocumento(conductor.getId(), tipo)
                .orElseGet(() -> DocumentoConductor.builder().conductor(conductor).tipoDocumento(tipo).build());
        borrarAnteriorTrasConfirmar(documento.getArchivoUrl());

        documento.setArchivoUrl(ruta);
        documento.setFechaVencimiento(fechaVencimiento);
        documento.setEstadoVerificacion(DocumentoConductor.EstadoVerificacion.PENDIENTE);
        return construir(documentoConductorRepository.save(documento), false);
    }

    private DocumentoResponse subirDocumentoVehiculo(Vehiculo vehiculo, DocumentoVehiculo.TipoDocumento tipo,
            byte[] contenido, TipoArchivo tipoArchivo, LocalDate fechaVencimiento) {
        String ruta = "vehiculos/" + vehiculo.getId() + "/" + UUID.randomUUID() + tipoArchivo.extension;
        storage.subir(ruta, contenido, tipoArchivo.contentType);
        borrarSiLaTransaccionFalla(ruta);

        DocumentoVehiculo documento = documentoVehiculoRepository
                .findByVehiculoIdAndTipoDocumento(vehiculo.getId(), tipo)
                .orElseGet(() -> DocumentoVehiculo.builder().vehiculo(vehiculo).tipoDocumento(tipo).build());
        borrarAnteriorTrasConfirmar(documento.getArchivoUrl());

        documento.setArchivoUrl(ruta);
        documento.setFechaVencimiento(fechaVencimiento);
        documento.setEstadoVerificacion(DocumentoVehiculo.EstadoVerificacion.PENDIENTE);
        return construir(documentoVehiculoRepository.save(documento), false);
    }

    // El archivo nuevo ya está en Storage; si la base falla, se borra para no dejarlo huérfano.
    private void borrarSiLaTransaccionFalla(String rutaNueva) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    storage.eliminarSilencioso(rutaNueva);
                }
            }
        });
    }

    // El archivo reemplazado solo se borra cuando el nuevo ya quedó registrado en la base.
    private void borrarAnteriorTrasConfirmar(String rutaAnterior) {
        if (rutaAnterior == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.eliminarSilencioso(rutaAnterior);
            }
        });
    }

    // ── Validaciones ────────────────────────────────────────────────────────

    private void validarFecha(String tipo, LocalDate fechaVencimiento) {
        if (!esTipoConductor(tipo) && !esTipoVehiculo(tipo)) {
            throw new DocumentoInvalidoException("Tipo de documento no válido. Usa: LICENCIA_CONDUCCION, "
                    + "SOAT, TECNOMECANICA, TARJETA_PROPIEDAD o TARJETA_OPERACION.");
        }
        if (TIPOS_CON_VENCIMIENTO.contains(tipo) && fechaVencimiento == null) {
            throw new DocumentoInvalidoException("La fecha de vencimiento es obligatoria para este documento.");
        }
        if (fechaVencimiento != null && !fechaVencimiento.isAfter(LocalDate.now())) {
            throw new DocumentoInvalidoException(
                    "La fecha de vencimiento debe ser posterior a hoy: el documento no puede estar vencido.");
        }
    }

    /** Valida el archivo por su contenido real (firma), no por la extensión ni el Content-Type del cliente. */
    private TipoArchivo validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DocumentoInvalidoException("El archivo está vacío. Selecciona un PDF, JPG o PNG.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new DocumentoInvalidoException("El archivo supera el máximo permitido de 5 MB.");
        }
        byte[] cabecera = new byte[8];
        int leidos;
        try (var entrada = archivo.getInputStream()) {
            leidos = entrada.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException e) {
            throw new DocumentoInvalidoException("No fue posible leer el archivo. Inténtalo de nuevo.");
        }
        return TipoArchivo.detectar(Arrays.copyOf(cabecera, leidos));
    }

    private byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new DocumentoInvalidoException("No fue posible leer el archivo. Inténtalo de nuevo.");
        }
    }

    // ── Pertenencia ─────────────────────────────────────────────────────────

    private Conductor obtenerConductorAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
        if (usuario.getTipoUsuario() != Usuario.TipoUsuario.CONDUCTOR) {
            throw new IllegalStateException("Solo los conductores pueden gestionar documentos");
        }
        return conductorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe un perfil de conductor asociado al usuario"));
    }

    // Un vehículo ajeno responde igual que uno inexistente, para no revelar cuáles existen.
    private Vehiculo obtenerVehiculoPropio(Conductor conductor, Long vehiculoId) {
        if (vehiculoId == null) {
            throw new DocumentoInvalidoException("Indica el vehículo al que pertenece el documento (vehiculoId).");
        }
        return vehiculoRepository.findById(vehiculoId)
                .filter(vehiculo -> vehiculo.getConductor().getId().equals(conductor.getId()))
                .orElseThrow(() -> new EntityNotFoundException("El vehículo no existe o no pertenece a tu cuenta"));
    }

    // ── Respuesta ───────────────────────────────────────────────────────────

    private DocumentoResponse construir(DocumentoConductor doc, boolean conEnlace) {
        DocumentoResponse.DocumentoResponseBuilder b = DocumentoResponse.builder()
                .id(doc.getId())
                .categoria("CONDUCTOR")
                .tipoDocumento(doc.getTipoDocumento().name())
                .estadoVerificacion(doc.getEstadoVerificacion().name())
                .fechaVencimiento(doc.getFechaVencimiento())
                .vencido(estaVencido(doc.getFechaVencimiento()));
        return conEnlaceFirmado(b, doc.getArchivoUrl(), conEnlace);
    }

    private DocumentoResponse construir(DocumentoVehiculo doc, boolean conEnlace) {
        DocumentoResponse.DocumentoResponseBuilder b = DocumentoResponse.builder()
                .id(doc.getId())
                .categoria("VEHICULO")
                .tipoDocumento(doc.getTipoDocumento().name())
                .vehiculoId(doc.getVehiculo().getId())
                .placa(doc.getVehiculo().getPlaca())
                .estadoVerificacion(doc.getEstadoVerificacion().name())
                .fechaVencimiento(doc.getFechaVencimiento())
                .vencido(estaVencido(doc.getFechaVencimiento()));
        return conEnlaceFirmado(b, doc.getArchivoUrl(), conEnlace);
    }

    // Si Storage falla al firmar, se devuelve el documento sin enlace en vez de romper el listado.
    private DocumentoResponse conEnlaceFirmado(DocumentoResponse.DocumentoResponseBuilder b, String ruta, boolean conEnlace) {
        if (conEnlace && storage.estaConfigurado()) {
            try {
                SupabaseStorageClient.EnlaceFirmado enlace = storage.firmar(ruta);
                b.enlaceVer(enlace.url()).enlaceExpiraEn(enlace.expiraEn());
            } catch (AlmacenamientoException e) {
                log.warn("Documento sin enlace: {}", e.getMessage());
            }
        }
        return b.build();
    }

    private static boolean estaVencido(LocalDate fecha) {
        return fecha != null && !fecha.isAfter(LocalDate.now());
    }

    private static boolean esTipoConductor(String tipo) {
        return "LICENCIA_CONDUCCION".equals(tipo);
    }

    private static boolean esTipoVehiculo(String tipo) {
        return Arrays.stream(DocumentoVehiculo.TipoDocumento.values()).anyMatch(t -> t.name().equals(tipo));
    }

    private static DocumentoVehiculo.TipoDocumento convertirTipoVehiculo(String tipo) {
        return DocumentoVehiculo.TipoDocumento.valueOf(tipo);
    }

    // ── Tipos de archivo permitidos (por firma / magic bytes) ───────────────

    private enum TipoArchivo {
        PDF("application/pdf", ".pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}),                       // %PDF-
        PNG("image/png", ".png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        JPG("image/jpeg", ".jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        private final String contentType;
        private final String extension;
        private final byte[] firma;

        TipoArchivo(String contentType, String extension, byte[] firma) {
            this.contentType = contentType;
            this.extension = extension;
            this.firma = firma;
        }

        static TipoArchivo detectar(byte[] cabecera) {
            for (TipoArchivo tipo : values()) {
                if (cabecera.length >= tipo.firma.length
                        && Arrays.equals(Arrays.copyOf(cabecera, tipo.firma.length), tipo.firma)) {
                    return tipo;
                }
            }
            throw new DocumentoInvalidoException("Tipo de archivo no permitido. Solo se aceptan PDF, JPG o PNG.");
        }
    }
}
