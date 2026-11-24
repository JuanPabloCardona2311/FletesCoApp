package com.example.msauth.controller;

import com.example.msauth.config.CorsConfig;
import com.example.msauth.entity.Usuario;
import com.example.msauth.repository.ConductorRepository;
import com.example.msauth.repository.DespachadorRepository;
import com.example.msauth.repository.UsuarioRepository;
import com.example.msauth.security.CookieJwtUtil;
import com.example.msauth.security.JwtService;
import com.example.msauth.security.SecurityConfig;
import com.example.msauth.security.UserDetailsServiceImpl;
import com.example.msauth.service.RecuperacionPasswordService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login, /me y /logout con la configuración de seguridad real (filtro JWT incluido).
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class, CookieJwtUtil.class})
class AuthCookieWebTest {

    // Valores ficticios: JwtService está simulado y no firma ni valida tokens reales.
    private static final String TOKEN = "token-de-prueba";
    private static final String CORREO = "despachador@ejemplo.test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private ConductorRepository conductorRepository;
    @MockitoBean
    private DespachadorRepository despachadorRepository;
    @MockitoBean
    private RecuperacionPasswordService recuperacionPasswordService;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = Usuario.builder().id(1L).email(CORREO).nombre("Laura Gómez")
                .passwordHash("hash-de-prueba").tipoUsuario(Usuario.TipoUsuario.DESPACHADOR)
                .estado(Usuario.EstadoUsuario.ACTIVO).build();
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario));
        when(jwtService.getJwtExpiration()).thenReturn(86_400_000L);
    }

    @Test
    void loginPoneLaCookieHttpOnlyYSigueDevolviendoElToken() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));
        when(jwtService.generateToken(any())).thenReturn(TOKEN);

        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CORREO + "\",\"password\":\"clave-de-prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andReturn();

        String cookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("FLETESCO_TOKEN=" + TOKEN)
                .contains("Path=/", "Max-Age=86400", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void meConCookieDevuelveElUsuarioSinTokenNiHash() throws Exception {
        autenticarConCookie();

        mockMvc.perform(get("/api/auth/me").cookie(new Cookie(CookieJwtUtil.NOMBRE_COOKIE, TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("DESPACHADOR"))
                .andExpect(jsonPath("$.email").value(CORREO))
                .andExpect(jsonPath("$.nombre").value("Laura Gómez"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meSinCookieResponde401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutBorraLaCookieAunqueNoHayaSesion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        String cookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("FLETESCO_TOKEN=;")
                .contains("Path=/", "Max-Age=0", "HttpOnly", "SameSite=Lax");
    }

    private void autenticarConCookie() {
        UserDetails detalles = User.withUsername(CORREO).password("hash-de-prueba")
                .authorities("ROLE_DESPACHADOR").build();
        when(jwtService.extractUsername(TOKEN)).thenReturn(CORREO);
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(detalles);
        when(jwtService.isTokenValid(TOKEN, detalles)).thenReturn(true);
    }
}
