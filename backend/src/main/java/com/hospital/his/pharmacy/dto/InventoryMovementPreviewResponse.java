package com.hospital.his.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Vista previa del efecto de un movimiento.
 *
 * No modifica la base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementPreviewResponse {

    private Long inventoryId;

    private Long branchId;

    private String branchName;

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String medicationUnit;

    private String movementType;

    /**
     * ENTRY u OUTPUT.
     */
    private String operationType;

    private Integer currentStock;

    private Integer minimumStock;

    private Integer quantity;

    private Integer signedQuantity;

    private Integer projectedStock;

    private Boolean lowStockWarning;

    private Boolean outOfStockWarning;

    private BigDecimal unitCost;

    private BigDecimal totalCost;

    private Long inventoryVersion;

    private String message;
}