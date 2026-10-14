package com.hospital.his.medicalagenda.entity;

/**
 * Tipos funcionales de eventos personales
 * de la agenda médica.
 *
 * Los valores corresponden con las opciones
 * definidas en CU-16.
 */
public enum MedicalAgendaEventType {

    /**
     * Reunión administrativa, clínica
     * o de coordinación.
     */
    REUNION,

    /**
     * Período de descanso del médico.
     */
    DESCANSO,

    /**
     * Capacitación, congreso, seminario
     * o actividad académica.
     */
    CAPACITACION,

    /**
     * Evento personal del médico.
     */
    PERSONAL,

    /**
     * Evento que no corresponde con las
     * categorías anteriores.
     */
    OTRO
}