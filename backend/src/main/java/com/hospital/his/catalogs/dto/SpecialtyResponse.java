package com.hospital.his.catalogs.dto;

import lombok.*;

/**
 * Respuesta completa del catálogo de especialidades.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {

    private Long id;

    private String name;

    private String description;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}