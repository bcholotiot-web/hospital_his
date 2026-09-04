package com.hospital.his.laboratory.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryTestResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private String referenceRange;

    private String defaultUnit;

    private BigDecimal price;
}