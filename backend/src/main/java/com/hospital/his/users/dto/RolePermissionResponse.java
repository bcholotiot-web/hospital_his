package com.hospital.his.users.dto;

import lombok.*;

/**
 * Muestra si un permiso está asignado
 * activamente a un rol.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionResponse {

    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String module;

    private String description;

    /**
     * true:
     * la relación role_permissions existe
     * y se encuentra activa.
     */
    private Boolean assigned;

    /**
     * ID de la fila en role_permissions.
     *
     * Será null cuando el permiso nunca haya
     * sido asignado al rol.
     */
    private Long rolePermissionId;

    private Long rolePermissionVersion;
}