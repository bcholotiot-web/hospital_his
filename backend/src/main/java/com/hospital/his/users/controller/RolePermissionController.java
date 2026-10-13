package com.hospital.his.users.controller;

import com.hospital.his.users.dto.PermissionResponse;
import com.hospital.his.users.dto.RolePermissionsResponse;
import com.hospital.his.users.dto.UpdateRolePermissionsRequest;
import com.hospital.his.users.service.RolePermissionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CU-14:
 * Administración de permisos granulares
 * asignados a roles.
 */
@RestController
@RequestMapping("/api/admin")
public class RolePermissionController {

    private final RolePermissionService
            rolePermissionService;

    public RolePermissionController(
            RolePermissionService rolePermissionService
    ) {
        this.rolePermissionService =
                rolePermissionService;
    }

    /**
     * Lista el catálogo de permisos activos.
     *
     * Ejemplos:
     *
     * GET /api/admin/permissions
     *
     * GET /api/admin/permissions
     *     ?module=MEDICATION
     */
    @PreAuthorize(
            "hasAuthority('PERMISSION_READ')"
    )
    @GetMapping("/permissions")
    public ResponseEntity<
            List<PermissionResponse>
            >
    getPermissions(
            @RequestParam(
                    required = false
            )
            String module
    ) {
        return ResponseEntity.ok(
                rolePermissionService
                        .getActivePermissions(
                                module
                        )
        );
    }

    /**
     * Obtiene los permisos asignados
     * y disponibles para un rol.
     */
    @PreAuthorize(
            "hasAuthority('PERMISSION_READ')"
    )
    @GetMapping(
            "/roles/{roleId}/permissions"
    )
    public ResponseEntity<
            RolePermissionsResponse
            >
    getRolePermissions(
            @PathVariable
            Long roleId
    ) {
        return ResponseEntity.ok(
                rolePermissionService
                        .getRolePermissions(
                                roleId
                        )
        );
    }

    /**
     * Reemplaza la selección completa
     * de permisos del rol.
     */
    @PreAuthorize(
            "hasAuthority('ROLE_PERMISSION_UPDATE')"
    )
    @PutMapping(
            "/roles/{roleId}/permissions"
    )
    public ResponseEntity<
            RolePermissionsResponse
            >
    updateRolePermissions(
            @PathVariable
            Long roleId,

            @Valid
            @RequestBody
            UpdateRolePermissionsRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                rolePermissionService
                        .updateRolePermissions(
                                roleId,
                                request,
                                authentication.getName()
                        )
        );
    }
}