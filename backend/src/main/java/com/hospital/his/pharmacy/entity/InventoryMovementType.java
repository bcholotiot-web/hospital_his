package com.hospital.his.pharmacy.entity;

/**
 * Tipos de movimientos que pueden modificar
 * las existencias de un medicamento.
 */
public enum InventoryMovementType {

    /**
     * Existencia registrada al crear por primera vez
     * el inventario del medicamento en una sucursal.
     */
    STOCK_INICIAL,

    /**
     * Ingreso de nuevas unidades al inventario.
     */
    ENTRADA,

    /**
     * Salida manual de unidades.
     *
     * No corresponde a un despacho de receta.
     */
    SALIDA,

    /**
     * Corrección que incrementa las existencias.
     */
    AJUSTE_POSITIVO,

    /**
     * Corrección que reduce las existencias.
     */
    AJUSTE_NEGATIVO,

    /**
     * Salida producida por un despacho
     * de medicamentos.
     */
    DESPACHO,

    /**
     * Reinicio o reactivación de un registro
     * de inventario previamente inactivo.
     */
    REACTIVACION
}