package com.hospital.his.users.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta completa de permisos disponibles
 * y asignados para un rol.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionsResponse {

    private Long roleId;

    private String roleName;

    private String roleDescription;

    private Boolean roleActive;

    /**
     * Indica si es un rol técnico utilizado
     * directamente por Spring Security.
     */
    private Boolean systemRole;

    private Integer assignedPermissionCount;

    private Integer availablePermissionCount;

    private List<RolePermissionResponse> permissions;

    private String message;
}