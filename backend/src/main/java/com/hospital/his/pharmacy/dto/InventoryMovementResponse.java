package com.hospital.his.pharmacy.dto;

import lombok.*;

/**
 * Respuesta de un movimiento de inventario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponse {

    private Long movementId;

    private Long inventoryId;

    private Long branchId;

    private String branchName;

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String medicationUnit;

    private String movementType;

    private Integer previousStock;

    private Integer quantity;

    /**
     * Representación firmada del movimiento.
     *
     * Ejemplos:
     *
     * +20 para ENTRADA.
     * -5 para SALIDA o DESPACHO.
     */
    private Integer signedQuantity;

    private Integer resultingStock;

    private String reason;

    private String referenceType;

    private Long referenceId;

    private String createdBy;

    private String createdAt;
}