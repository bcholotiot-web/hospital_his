package com.hospital.his.laboratory.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Respuesta completa de un examen de laboratorio.
 *
 * Los campos adicionales no afectan el modal médico,
 * que puede continuar usando código, nombre y precio.
 */
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

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}