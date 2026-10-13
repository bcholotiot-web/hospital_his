package com.hospital.his.users.controller;

import com.hospital.his.users.dto.CreateRoleRequest;
import com.hospital.his.users.dto.RolePageResponse;
import com.hospital.his.users.dto.RoleResponse;
import com.hospital.his.users.dto.UpdateRoleRequest;
import com.hospital.his.users.service.RoleService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(
            RoleService roleService
    ) {
        this.roleService =
                roleService;
    }

    /**
     * Conserva el listado administrativo completo.
     */
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping
    public ResponseEntity<List<RoleResponse>>
    getRoles() {
        return ResponseEntity.ok(
                roleService.getAllRoles()
        );
    }

    /**
     * Listado paginado con filtros.
     */
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping("/page")
    public ResponseEntity<RolePageResponse>
    getRolesPage(
            @RequestParam(
                    required = false
            )
            String name,

            @RequestParam(
                    required = false
            )
            Boolean active,

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "20"
            )
            Integer size
    ) {
        return ResponseEntity.ok(
                roleService.getRolesPage(
                        name,
                        active,
                        page,
                        size
                )
        );
    }

    /**
     * Roles activos disponibles para asignar
     * a usuarios.
     */
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping("/active")
    public ResponseEntity<List<RoleResponse>>
    getActiveRoles() {
        return ResponseEntity.ok(
                roleService.getActiveRoles()
        );
    }

    /**
     * Busca un rol por ID.
     */
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse>
    getRoleById(
            @PathVariable
            Long roleId
    ) {
        return ResponseEntity.ok(
                roleService.getRoleById(
                        roleId
                )
        );
    }

    /**
     * Crea o reactiva un rol.
     */
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @PostMapping
    public ResponseEntity<RoleResponse>
    createRole(
            @Valid
            @RequestBody
            CreateRoleRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                roleService.createRole(
                        request,
                        authentication.getName()
                )
        );
    }

    /**
     * Actualiza nombre y descripción.
     */
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse>
    updateRole(
            @PathVariable
            Long roleId,

            @Valid
            @RequestBody
            UpdateRoleRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                roleService.updateRole(
                        roleId,
                        request,
                        authentication.getName()
                )
        );
    }

    /**
     * Activa o desactiva un rol.
     */
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    @PatchMapping("/{roleId}/status")
    public ResponseEntity<RoleResponse>
    changeRoleStatus(
            @PathVariable
            Long roleId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                roleService.changeStatus(
                        roleId,
                        active,
                        version,
                        authentication.getName()
                )
        );
    }
}