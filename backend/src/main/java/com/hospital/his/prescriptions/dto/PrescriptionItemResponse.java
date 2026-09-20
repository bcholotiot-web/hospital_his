package com.hospital.his.prescriptions.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemResponse {

    private Long prescriptionItemId;

    private Long medicationId;

    private String medicationCode;

    private String medicationName;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    private BigDecimal currentUnitPrice;

    private String dosage;

    private String frequency;

    private String duration;

    private Integer prescribedQuantity;

    private Integer dispensedQuantity;

    /**
     * Cantidad que todavía puede ser entregada.
     */
    private Integer pendingQuantity;

    private String specialInstructions;

    private Boolean controlledMedication;

    private Boolean fullyDispensed;

    private Boolean active;
}