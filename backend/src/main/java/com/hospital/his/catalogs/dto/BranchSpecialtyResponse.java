package com.hospital.his.catalogs.dto;

import lombok.*;

/**
 * Respuesta administrativa de una asignación
 * sede-especialidad.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSpecialtyResponse {

    private Long id;

    private Long branchId;

    private String branchName;

    private String branchAddress;

    private Long specialtyId;

    private String specialtyName;

    private String specialtyDescription;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}