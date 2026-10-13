package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.util.List;

/**
 * Resumen mensual del movimiento de inventario
 * de una sucursal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMonthlySummaryResponse {

    private Long branchId;

    private String branchName;

    private Integer year;

    private Integer month;

    private String periodStart;

    private String periodEnd;

    private Integer totalMovements;

    private Integer totalEntries;

    private Integer totalOutputs;

    private Integer totalDispensed;

    private List<InventoryMonthlySummaryItemResponse>
            medications;
}