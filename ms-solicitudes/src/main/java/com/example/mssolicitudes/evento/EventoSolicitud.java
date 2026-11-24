package com.example.mssolicitudes.evento;

// Aviso de que una solicitud cambió. Solo lleva ids y estados: nada de nombres,
// teléfonos ni precios. Quien lo recibe vuelve a leer la base de datos si necesita más.
public record EventoSolicitud(String tipo, Long solicitudId, String estadoAnterior, String estadoNuevo,
                              Long despachadorId, Long conductorId, String fecha) {

    public static final String SOLICITUD_CREADA = "SOLICITUD_CREADA";
    public static final String SOLICITUD_ACEPTADA = "SOLICITUD_ACEPTADA";
    public static final String VIAJE_INICIADO = "VIAJE_INICIADO";
    public static final String ENTREGA_MARCADA = "ENTREGA_MARCADA";
    public static final String PAGO_LIBERADO = "PAGO_LIBERADO";
}
