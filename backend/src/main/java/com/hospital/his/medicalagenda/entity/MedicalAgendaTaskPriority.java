package com.hospital.his.medicalagenda.entity;

/**
 * Prioridad de una tarea o recordatorio
 * dentro de la agenda médica.
 */
public enum MedicalAgendaTaskPriority {

    /**
     * Tarea de prioridad baja.
     */
    BAJA,

    /**
     * Prioridad predeterminada.
     */
    NORMAL,

    /**
     * Tarea que requiere atención prioritaria.
     */
    ALTA
}