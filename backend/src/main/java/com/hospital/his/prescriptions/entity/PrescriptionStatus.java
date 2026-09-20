package com.hospital.his.prescriptions.entity;

/**
 * Estados funcionales de una receta médica.
 */
public enum PrescriptionStatus {

    /**
     * La receta fue emitida y todavía puede ser
     * atendida por Farmacia.
     */
    ACTIVA,

    /**
     * Se entregó una parte de los medicamentos,
     * pero todavía existen cantidades pendientes.
     */
    PARCIALMENTE_DESPACHADA,

    /**
     * Todos los medicamentos fueron entregados.
     */
    DESPACHADA,

    /**
     * La receta fue cancelada antes de completar
     * el despacho.
     */
    CANCELADA
}