package com.example.msauth.dto.response;

// Datos del usuario de la sesión para GET /api/auth/me. Nunca incluye el token ni el hash.
public record UsuarioActualResponse(String tipoUsuario, String email, String nombre) {
}
