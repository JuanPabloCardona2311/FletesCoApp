package com.example.msauth;

import com.example.msauth.entity.PasswordResetToken;
import com.example.msauth.entity.Usuario;
import com.example.msauth.repository.PasswordResetTokenRepository;
import com.example.msauth.repository.UsuarioRepository;
import com.example.msauth.service.EnvioEnlaceRecuperacion;
import com.example.msauth.service.RecuperacionPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:recuperaciondb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac-sha",
        "jwt.expiration=86400000"
})
class RecuperacionPasswordServiceTest {

    private static final String PASSWORD_ORIGINAL = "original123";

    @Autowired
    private RecuperacionPasswordService servicio;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Sustituye al envío simulado para capturar el enlace sin leer el log.
    @MockitoBean
    private EnvioEnlaceRecuperacion envioEnlace;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        tokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        clearInvocations(envioEnlace);
        usuario = crearUsuario("activo@fletesco.test", "1000000001", Usuario.EstadoUsuario.ACTIVO);
    }

    @Test
    void correoInexistenteNoEmiteEnlace() {
        servicio.solicitarRecuperacion("no-existe@fletesco.test");

        verify(envioEnlace, never()).enviar(any(), any());
        assertThat(tokenRepository.count()).isZero();
    }

    @Test
    void usuarioInactivoNoEmiteEnlace() {
        crearUsuario("inactivo@fletesco.test", "1000000002", Usuario.EstadoUsuario.INACTIVO);

        servicio.solicitarRecuperacion("inactivo@fletesco.test");

        verify(envioEnlace, never()).enviar(any(), any());
    }

    @Test
    void guardaSoloElHashYRestableceLaPasswordUnaSolaVez() {
        String token = solicitarYCapturarToken();

        PasswordResetToken guardado = tokenRepository.findAll().get(0);
        assertThat(guardado.getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(guardado.getExpiraEn()).isAfter(Instant.now().plusSeconds(29 * 60));

        servicio.restablecerPassword(token, "nueva-clave-123");

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nueva-clave-123", actualizado.getPasswordHash())).isTrue();
        assertThat(tokenRepository.findAll().get(0).isUsado()).isTrue();

        assertThatThrownBy(() -> servicio.restablecerPassword(token, "otra-clave-456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue usado");
    }

    @Test
    void enlaceVencidoSeRechaza() {
        String token = solicitarYCapturarToken();
        PasswordResetToken guardado = tokenRepository.findAll().get(0);
        guardado.setExpiraEn(Instant.now().minusSeconds(60));
        tokenRepository.save(guardado);

        assertThatThrownBy(() -> servicio.restablecerPassword(token, "nueva-clave-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("venció");

        Usuario sinCambios = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(PASSWORD_ORIGINAL, sinCambios.getPasswordHash())).isTrue();
    }

    @Test
    void tokenInventadoSeRechaza() {
        assertThatThrownBy(() -> servicio.restablecerPassword("token-que-no-existe", "nueva-clave-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es válido");
    }

    @Test
    void pedirUnEnlaceNuevoInvalidaElAnterior() {
        String primero = solicitarYCapturarToken();
        String segundo = solicitarYCapturarToken();

        assertThatThrownBy(() -> servicio.restablecerPassword(primero, "nueva-clave-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue usado");

        servicio.restablecerPassword(segundo, "nueva-clave-123");
    }

    private String solicitarYCapturarToken() {
        clearInvocations(envioEnlace);
        servicio.solicitarRecuperacion(usuario.getEmail());

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(envioEnlace, times(1)).enviar(any(), enlace.capture());
        assertThat(enlace.getValue()).startsWith("http://localhost:5173/restablecer?token=");
        return enlace.getValue().substring(enlace.getValue().indexOf("token=") + "token=".length());
    }

    private Usuario crearUsuario(String email, String documento, Usuario.EstadoUsuario estado) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Prueba Recuperacion")
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD_ORIGINAL))
                .telefono("3000000000")
                .tipoDocumentoIdentidad(Usuario.TipoDocumentoIdentidad.CC)
                .numeroDocumentoIdentidad(documento)
                .tipoUsuario(Usuario.TipoUsuario.CONDUCTOR)
                .fechaRegistro(LocalDateTime.now())
                .estado(estado)
                .build());
    }
}
