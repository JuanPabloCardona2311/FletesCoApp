package com.example.msauth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Crea y borra la cookie HttpOnly con el JWT. HttpOnly: ningún JavaScript de la página
// puede leerla (protege el token ante XSS). SameSite=Lax: no viaja en peticiones
// POST/PUT/PATCH/DELETE que otros sitios inicien. Secure se activa en el servidor con
// APP_COOKIE_SECURE=true (HTTPS); en desarrollo local, con http, va en false.
@Component
public class CookieJwtUtil {

    public static final String NOMBRE_COOKIE = "FLETESCO_TOKEN";

    private final JwtService jwtService;
    private final boolean secure;

    public CookieJwtUtil(JwtService jwtService, @Value("${app.cookie.secure:false}") boolean secure) {
        this.jwtService = jwtService;
        this.secure = secure;
    }

    // Cookie con el token; dura lo mismo que el JWT (jwt.expiration).
    public ResponseCookie crear(String token) {
        return base(token).maxAge(Duration.ofMillis(jwtService.getJwtExpiration())).build();
    }

    // Misma cookie (nombre, ruta y flags) vacía y con Max-Age=0: el navegador la borra.
    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE_COOKIE, valor)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax");
    }
}
