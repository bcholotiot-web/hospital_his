package com.hospital.his.catalogs.dto;

import lombok.*;

/**
 * Respuesta completa de una sucursal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

    private Long id;

    private String name;

    private String phone;

    private String address;

    private String description;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}