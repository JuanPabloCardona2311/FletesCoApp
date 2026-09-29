package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.Conductor;
import com.example.demo.entity.Despachador;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.ConductorRepository;
import com.example.demo.repository.DespachadorRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final DespachadorRepository despachadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            DespachadorRepository despachadorRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.despachadorRepository = despachadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya está registrado");
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

        return ResponseEntity.status(HttpStatus.CREATED).body(
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

            return ResponseEntity.ok(new AuthResponse(token, usuario.getTipoUsuario().name(), usuario.getEmail(), usuario.getNombre()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }
}
