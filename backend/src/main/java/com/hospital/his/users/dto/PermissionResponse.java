package com.hospital.his.users.dto;

import lombok.*;

/**
 * Respuesta de un permiso técnico activo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private Long id;

    private String code;

    private String name;

    private String module;

    private String description;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;
}