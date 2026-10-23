package com.example.msperfil.exception;

/** Archivo o datos del documento rechazados (vacío, tipo no permitido, fecha inválida...). Responde 400. */
public class DocumentoInvalidoException extends RuntimeException {
    public DocumentoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
