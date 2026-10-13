package com.hospital.his.medications.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Respuesta completa de un medicamento.
 *
 * Continúa siendo compatible con el modal médico
 * utilizado para generar recetas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationResponse {

    private Long id;

    private String code;

    private String name;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    private String unit;

    private String description;

    private BigDecimal unitPrice;

    private Boolean controlled;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}