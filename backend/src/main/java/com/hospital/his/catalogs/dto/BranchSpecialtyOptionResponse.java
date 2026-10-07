package com.hospital.his.catalogs.dto;

import lombok.*;

/**
 * DTO reducido para mostrar opciones activas
 * dentro de formularios.
 *
 * Puede representar una sede o una especialidad.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSpecialtyOptionResponse {

    private Long id;

    private String name;

    private String description;
}