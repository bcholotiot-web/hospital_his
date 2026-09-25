package com.hospital.his.appointments.entity;

/**
 * Estados del recordatorio de una cita.
 */
public enum AppointmentReminderStatus {

    /**
     * El recordatorio está pendiente de envío.
     */
    PENDIENTE,

    /**
     * El correo fue enviado correctamente.
     */
    ENVIADO,

    /**
     * No se pudo enviar después del intento.
     */
    ERROR,

    /**
     * La cita fue cancelada o el recordatorio
     * ya no debe enviarse.
     */
    CANCELADO
}