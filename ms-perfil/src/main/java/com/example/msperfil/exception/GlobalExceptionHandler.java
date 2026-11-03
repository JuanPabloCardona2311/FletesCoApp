package com.example.msperfil.exception;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> response = base(HttpStatus.BAD_REQUEST);
        response.put("error", "Error de validación");
        response.put("errores", errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    // ── Documentos (E2-02 / E2-03) ──────────────────────────────────────────

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> documentoInvalido(DocumentoInvalidoException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        return body(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el máximo permitido de 5 MB.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> faltaArchivo(MissingServletRequestPartException ex) {
        return body(HttpStatus.BAD_REQUEST, "Debes adjuntar un archivo (PDF, JPG o PNG).");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> faltaParametro(MissingServletRequestParameterException ex) {
        return body(HttpStatus.BAD_REQUEST, "Falta el dato obligatorio '" + ex.getParameterName() + "'.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "fechaVencimiento".equals(ex.getName())
                ? "La fecha de vencimiento no es válida. Usa el formato AAAA-MM-DD."
                : "El valor de '" + ex.getName() + "' no es válido.";
        return body(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(AlmacenamientoException.class)
    public ResponseEntity<Map<String, Object>> almacenamiento(AlmacenamientoException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception ex) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error) {
        Map<String, Object> response = base(status);
        response.put("error", error);
        return ResponseEntity.status(status).body(response);
    }

    private Map<String, Object> base(HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        return response;
    }
}
