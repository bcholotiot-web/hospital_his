package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.math.BigDecimal;

//Para medicamentos alternativos
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativeMedicationResponse {

    private Long medicationId;

    private String code;

    private String name;

    private String activeIngredient;

    private String concentration;

    private String presentation;

    private BigDecimal unitPrice;

    private Integer availableStock;

    private Boolean controlled;

    private Boolean lowStock;
}