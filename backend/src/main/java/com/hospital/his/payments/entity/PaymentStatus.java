package com.hospital.his.payments.entity;

public enum PaymentStatus {

    /**
     PENDIENTE    → Pago creado, todavía no procesado
     PROCESANDO   → Solicitud enviada a la pasarela
     APROBADO     → Pago exitoso
     RECHAZADO    → Pasarela o banco rechazó la operación
     ERROR        → Fallo técnico o de comunicación
     * */
    PENDIENTE,

    PROCESANDO,

    APROBADO,

    RECHAZADO,

    ERROR
}