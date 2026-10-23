package com.example.msauth.service;

import com.example.msauth.entity.PasswordResetToken;
import com.example.msauth.entity.Usuario;
import com.example.msauth.repository.PasswordResetTokenRepository;
import com.example.msauth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperación de contraseña (E1-05): emisión de enlaces de un solo uso y
 * cambio de contraseña con ellos.
 */
@Service
public class RecuperacionPasswordService {

    private static final int BYTES_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEnlaceRecuperacion envioEnlace;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String urlFrontend;
    private final Duration validez;

    public RecuperacionPasswordService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EnvioEnlaceRecuperacion envioEnlace,
            @Value("${app.recuperacion.url-frontend:http://localhost:5173}") String urlFrontend,
            @Value("${app.recuperacion.minutos-validez:30}") long minutosValidez
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioEnlace = envioEnlace;
        this.urlFrontend = urlFrontend;
        this.validez = Duration.ofMinutes(minutosValidez);
    }

    /**
     * Si el email pertenece a un usuario ACTIVO, emite un enlace nuevo e invalida
     * los anteriores. Si no, no hace nada: el controlador responde lo mismo en
     * ambos casos para no revelar qué correos están registrados.
     */
    @Transactional
    public void solicitarRecuperacion(String email) {
        usuarioRepository.findByEmail(email.trim())
                .filter(usuario -> usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO)
                .ifPresent(this::emitirEnlace);
    }

    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashear(token))
                .orElseThrow(() -> new IllegalStateException(
                        "El enlace de recuperación no es válido. Solicita uno nuevo."));

        if (resetToken.isUsado()) {
            throw new IllegalStateException("Este enlace de recuperación ya fue usado. Solicita uno nuevo.");
        }
        if (Instant.now().isAfter(resetToken.getExpiraEn())) {
            throw new IllegalStateException("El enlace de recuperación venció. Solicita uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        if (usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            throw new IllegalStateException("La cuenta no está activa.");
        }

        // Marca el token antes de cambiar la contraseña; si otra petición con el
        // mismo enlace se adelantó, esta no actualiza nada y se rechaza.
        if (tokenRepository.marcarComoUsado(resetToken.getId()) == 0) {
            throw new IllegalStateException("Este enlace de recuperación ya fue usado. Solicita uno nuevo.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
    }

    private void emitirEnlace(Usuario usuario) {
        tokenRepository.invalidarPendientes(usuario.getId());

        byte[] aleatorio = new byte[BYTES_TOKEN];
        secureRandom.nextBytes(aleatorio);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        Instant ahora = Instant.now();
        tokenRepository.save(PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash(hashear(token))
                .expiraEn(ahora.plus(validez))
                .usado(false)
                .creadoEn(ahora)
                .build());

        envioEnlace.enviar(usuario, urlFrontend + "/restablecer?token=" + token);
    }

    private static String hashear(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
        }
    }
}
