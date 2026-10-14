package com.hospital.his.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Detalle completo de un movimiento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementDetailResponse {

    private Long movementId;

    private Long inventoryId;

    private Long branchId;

    private String branchName;

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    private String medicationUnit;

    private String movementType;

    private String operationType;

    private Integer previousStock;

    private Integer quantity;

    private Integer signedQuantity;

    private Integer resultingStock;

    private BigDecimal unitCost;

    private BigDecimal totalCost;

    private String reason;

    private String referenceType;

    private Long referenceId;

    private String referenceNumber;

    private Boolean active;

    private String createdBy;

    private String createdAt;

    private String updatedBy;

    private String updatedAt;

    private Long version;
}