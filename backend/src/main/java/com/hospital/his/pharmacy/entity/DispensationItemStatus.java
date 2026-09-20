package com.hospital.his.pharmacy.entity;

/**
 * Estado de cada medicamento incluido en el despacho.
 */
public enum DispensationItemStatus {

    /**
     * Se entregó toda la cantidad pendiente.
     */
    COMPLETO,

    /**
     * Se entregó una cantidad menor a la pendiente.
     */
    PARCIAL,

    /**
     * Se entregó otro medicamento en sustitución
     * del medicamento originalmente prescrito.
     */
    SUSTITUIDO,

    /**
     * El medicamento no fue entregado.
     */
    NO_DESPACHADO
}