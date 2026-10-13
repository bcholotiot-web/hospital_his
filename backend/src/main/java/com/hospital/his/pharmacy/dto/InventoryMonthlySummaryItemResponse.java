package com.hospital.his.pharmacy.dto;

import lombok.*;

/**
 * Resumen mensual de un medicamento específico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMonthlySummaryItemResponse {

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String medicationUnit;

    private Integer initialStock;

    private Integer totalEntries;

    private Integer totalManualOutputs;

    private Integer totalDispensed;

    private Integer totalPositiveAdjustments;

    private Integer totalNegativeAdjustments;

    private Integer finalStock;

    private Integer movementCount;
}