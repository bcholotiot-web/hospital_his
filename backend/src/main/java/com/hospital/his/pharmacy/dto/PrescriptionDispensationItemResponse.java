package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

//Inventario por medicamento recetado

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDispensationItemResponse {

    private Long prescriptionItemId;

    private Long originalMedicationId;

    private String medicationCode;

    private String medicationName;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    private String dosage;

    private String frequency;

    private String duration;

    private String specialInstructions;

    private Integer prescribedQuantity;

    private Integer alreadyDispensedQuantity;

    private Integer pendingQuantity;

    private Boolean controlledMedication;

    /**
     * Precio actual del medicamento original.
     */
    private BigDecimal currentUnitPrice;

    /**
     * Stock disponible en la sucursal.
     */
    private Integer availableStock;

    private Integer minimumStock;

    private Boolean available;

    private Boolean enoughForFullDispensation;

    private Boolean lowStock;

    /**
     * Medicamentos alternativos disponibles.
     *
     * En esta etapa solo se devuelven alternativas
     * del mismo principio activo.
     */
    private List<AlternativeMedicationResponse>
            alternatives;

    private String message;
}