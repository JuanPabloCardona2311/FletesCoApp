package com.example.msperfil.exception;

/** Supabase Storage no está configurado o no respondió bien. Responde 503 con un mensaje para el usuario. */
public class AlmacenamientoException extends RuntimeException {
    public AlmacenamientoException(String mensaje) {
        super(mensaje);
    }

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
