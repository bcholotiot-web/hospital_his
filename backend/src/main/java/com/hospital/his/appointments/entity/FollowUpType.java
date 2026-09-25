package com.hospital.his.appointments.entity;

/**
 * Define el motivo funcional de una cita
 * de seguimiento médico.
 */
public enum FollowUpType {

    /**
     * Seguimiento de la respuesta del paciente
     * al tratamiento indicado en la consulta anterior.
     */
    MONITOREO_TRATAMIENTO,

    /**
     * Cita posterior para revisar resultados
     * de laboratorio.
     */
    REVISION_RESULTADOS_LABORATORIO
}