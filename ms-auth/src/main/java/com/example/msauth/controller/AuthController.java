package com.example.msauth.controller;

import com.example.msauth.dto.request.LoginRequest;
import com.example.msauth.dto.request.RecuperarPasswordRequest;
import com.example.msauth.dto.request.RegisterRequest;
import com.example.msauth.dto.request.RestablecerPasswordRequest;
import com.example.msauth.dto.response.AuthResponse;
import com.example.msauth.dto.response.UsuarioActualResponse;
import com.example.msauth.entity.Conductor;
import com.example.msauth.entity.Despachador;
import com.example.msauth.entity.Usuario;
import com.example.msauth.repository.ConductorRepository;
import com.example.msauth.repository.DespachadorRepository;
import com.example.msauth.repository.UsuarioRepository;
import com.example.msauth.security.CookieJwtUtil;
import com.example.msauth.security.JwtService;
import com.example.msauth.service.RecuperacionPasswordService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MENSAJE_RECUPERACION =
            "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final DespachadorRepository despachadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RecuperacionPasswordService recuperacionPasswordService;
    private final CookieJwtUtil cookieJwtUtil;

    public AuthController(
            UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            DespachadorRepository despachadorRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RecuperacionPasswordService recuperacionPasswordService,
            CookieJwtUtil cookieJwtUtil
    ) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.despachadorRepository = despachadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.recuperacionPasswordService = recuperacionPasswordService;
        this.cookieJwtUtil = cookieJwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya está registrado");
        }

        if (request.getTipoUsuario() == Usuario.TipoUsuario.ADMINISTRADOR) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No es posible registrarse con el rol ADMINISTRADOR");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .tipoDocumentoIdentidad(request.getTipoDocumentoIdentidad())
                .numeroDocumentoIdentidad(request.getNumeroDocumentoIdentidad())
                .tipoUsuario(request.getTipoUsuario())
                .fechaRegistro(LocalDateTime.now())
                .estado(Usuario.EstadoUsuario.ACTIVO)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        if (usuarioGuardado.getTipoUsuario() == Usuario.TipoUsuario.CONDUCTOR) {
            Conductor conductor = Conductor.builder()
                    .usuario(usuarioGuardado)
                    .cancelacionesTotales(0)
                    .build();
            conductorRepository.save(conductor);
        }

        if (usuarioGuardado.getTipoUsuario() == Usuario.TipoUsuario.DESPACHADOR) {
            Despachador despachador = Despachador.builder()
                    .usuario(usuarioGuardado)
                    .build();
            despachadorRepository.save(despachador);
        }

        String token = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                usuarioGuardado.getEmail(),
                usuarioGuardado.getPasswordHash(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuarioGuardado.getTipoUsuario().name()))
        ));

        // El token va en la cookie HttpOnly y, por compatibilidad con el frontend actual,
        // también en el body (se quita en la parte 2).
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieJwtUtil.crear(token).toString())
                .body(
                new AuthResponse(token, usuarioGuardado.getTipoUsuario().name(), usuarioGuardado.getEmail(), usuarioGuardado.getNombre())
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (authentication.isAuthenticated()) {
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String token = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                    usuario.getEmail(),
                    usuario.getPasswordHash(),
                    java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuario.getTipoUsuario().name()))
            ));

            // Cookie HttpOnly + token en el body por compatibilidad (se quita en la parte 2).
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieJwtUtil.crear(token).toString())
                    .body(new AuthResponse(token, usuario.getTipoUsuario().name(), usuario.getEmail(), usuario.getNombre()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }

    // Usuario de la sesión (cookie FLETESCO_TOKEN o header Bearer). Lo usa el frontend para
    // saber quién está conectado sin guardar datos en localStorage.
    @GetMapping("/me")
    public ResponseEntity<UsuarioActualResponse> me(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return ResponseEntity.ok(new UsuarioActualResponse(
                usuario.getTipoUsuario().name(), usuario.getEmail(), usuario.getNombre()));
    }

    // Cierra la sesión borrando la cookie. Es público: funciona aunque el token ya venció.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieJwtUtil.borrar().toString())
                .build();
    }

    /**
     * E1-05: solicitar el enlace de recuperación. Responde siempre lo mismo,
     * exista o no el correo, para no revelar qué cuentas están registradas.
     */
    @PostMapping("/recuperar")
    public ResponseEntity<Map<String, String>> recuperar(@Valid @RequestBody RecuperarPasswordRequest request) {
        recuperacionPasswordService.solicitarRecuperacion(request.getEmail());
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_RECUPERACION));
    }

    /**
     * E1-05: cambiar la contraseña con el enlace recibido. Errores de enlace
     * inválido, usado o vencido llegan como 400 desde GlobalExceptionHandler.
     */
    @PostMapping("/restablecer")
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacionPasswordService.restablecerPassword(request.getToken(), request.getNuevaPassword());
        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó. Ya puedes iniciar sesión."));
    }
}
