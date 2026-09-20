package com.hospital.his.pharmacy.entity;

/**
 * Estados generales de un despacho de medicamentos.
 */
public enum DispensationStatus {

    /**
     * El despacho fue creado y se encuentra
     * en proceso de validación y cobro.
     */
    PROCESANDO,

    /**
     * El despacho fue completado correctamente.
     *
     * El inventario ya fue descontado.
     */
    CONFIRMADO,

    /**
     * El paciente decidió no adquirir los medicamentos
     * o el despacho fue cancelado antes de confirmarse.
     */
    CANCELADO,

    /**
     * Ocurrió un error técnico durante el procesamiento.
     */
    ERROR
}