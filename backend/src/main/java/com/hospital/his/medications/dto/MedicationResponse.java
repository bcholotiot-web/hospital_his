package com.hospital.his.medications.dto;

import lombok.*;

import java.math.BigDecimal;

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

    private String description;

    private BigDecimal unitPrice;

    private Boolean controlled;

    private Boolean active;
}