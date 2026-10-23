package com.example.msauth.service;

import com.example.msauth.entity.Usuario;

/**
 * Entrega al usuario el enlace para restablecer su contraseña.
 * Hoy solo existe la versión simulada (log). Un envío real por correo debe
 * implementar esta interfaz y activarse con app.recuperacion.simulada=false.
 */
public interface EnvioEnlaceRecuperacion {

    void enviar(Usuario usuario, String enlace);
}
