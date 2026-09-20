package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.math.BigDecimal;

//Respuesta del detalle despachado
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDispensationItemResponse {

    private Long dispensationItemId;

    private Long prescriptionItemId;

    private Long originalMedicationId;

    private String originalMedicationName;

    private Long dispensedMedicationId;

    private String dispensedMedicationName;

    private Integer prescribedQuantity;

    private Integer pendingQuantityBefore;

    private Integer dispensedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    private Integer stockBefore;

    private Integer stockAfter;

    private Boolean substituted;

    private String substitutionReason;

    private Boolean controlledMedication;

    private String status;

    private Boolean lowStockAfterDispensation;

    private String stockAlert;
}