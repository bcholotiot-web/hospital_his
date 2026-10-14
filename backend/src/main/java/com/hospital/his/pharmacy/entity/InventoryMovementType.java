package com.hospital.his.pharmacy.entity;

/**
 * Tipos de movimientos que pueden modificar
 * las existencias de un medicamento.
 *
 * Existen dos grupos:
 *
 * 1. Movimientos manuales:
 *    COMPRA
 *    DEVOLUCION
 *    VENTA
 *    RECLAMO
 *    AJUSTE_POSITIVO
 *    AJUSTE_NEGATIVO
 *
 * 2. Movimientos generados por otros procesos:
 *    STOCK_INICIAL
 *    ENTRADA
 *    SALIDA
 *    DESPACHO
 *    REACTIVACION
 */
public enum InventoryMovementType {

    /**
     * Existencia registrada al crear por primera vez
     * el inventario del medicamento.
     */
    STOCK_INICIAL,

    /**
     * Entrada histórica o genérica.
     *
     * Se conserva por compatibilidad con movimientos
     * registrados antes de CU-15.
     */
    ENTRADA,

    /**
     * Salida histórica o genérica.
     *
     * Se conserva por compatibilidad con movimientos
     * registrados antes de CU-15.
     */
    SALIDA,

    /**
     * Ingreso por compra a proveedor.
     *
     * Requiere costo unitario.
     */
    COMPRA,

    /**
     * Ingreso de existencias devueltas
     * al inventario.
     */
    DEVOLUCION,

    /**
     * Salida manual asociada con una venta.
     */
    VENTA,

    /**
     * Salida de existencias asociada con
     * un reclamo, daño o incidencia.
     */
    RECLAMO,

    /**
     * Corrección manual que incrementa
     * las existencias.
     */
    AJUSTE_POSITIVO,

    /**
     * Corrección manual que reduce
     * las existencias.
     */
    AJUSTE_NEGATIVO,

    /**
     * Salida producida automáticamente por
     * el despacho de una receta.
     *
     * No debe aparecer como opción en el
     * formulario manual.
     */
    DESPACHO,

    /**
     * Reactivación de un registro de inventario
     * previamente inactivo.
     */
    REACTIVACION;

    /**
     * Indica si el tipo incrementa el stock.
     */
    public boolean isEntry() {
        return switch (this) {
            case STOCK_INICIAL,
                 ENTRADA,
                 COMPRA,
                 DEVOLUCION,
                 AJUSTE_POSITIVO,
                 REACTIVACION -> true;

            default -> false;
        };
    }

    /**
     * Indica si el tipo reduce el stock.
     */
    public boolean isOutput() {
        return switch (this) {
            case SALIDA,
                 VENTA,
                 RECLAMO,
                 AJUSTE_NEGATIVO,
                 DESPACHO -> true;

            default -> false;
        };
    }

    /**
     * Indica si el tipo puede ser registrado
     * manualmente desde CU-15.
     */
    public boolean isManual() {
        return switch (this) {
            case COMPRA,
                 DEVOLUCION,
                 VENTA,
                 RECLAMO,
                 AJUSTE_POSITIVO,
                 AJUSTE_NEGATIVO -> true;

            default -> false;
        };
    }

    /**
     * Indica si el movimiento exige
     * costo unitario.
     */
    public boolean requiresUnitCost() {
        return this == COMPRA;
    }

    /**
     * Indica si el movimiento exige motivo.
     */
    public boolean requiresReason() {
        return switch (this) {
            case DEVOLUCION,
                 RECLAMO,
                 AJUSTE_POSITIVO,
                 AJUSTE_NEGATIVO -> true;

            default -> false;
        };
    }
}