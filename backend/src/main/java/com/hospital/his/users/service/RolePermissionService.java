package com.hospital.his.users.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.users.dto.PermissionResponse;
import com.hospital.his.users.dto.RolePermissionResponse;
import com.hospital.his.users.dto.RolePermissionsResponse;
import com.hospital.his.users.dto.UpdateRolePermissionsRequest;
import com.hospital.his.users.entity.Permission;
import com.hospital.his.users.entity.Role;
import com.hospital.his.users.entity.RolePermission;
import com.hospital.his.users.repository.PermissionRepository;
import com.hospital.his.users.repository.RolePermissionRepository;
import com.hospital.his.users.repository.RoleRepository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {

    /**
     * Permisos mínimos que ADMIN no puede perder.
     *
     * Así evitamos que el administrador se quede
     * sin acceso a la administración de seguridad.
     */
    private static final Set<String>
            REQUIRED_ADMIN_PERMISSIONS =
            Set.of(
                    "PERMISSION_READ",
                    "ROLE_PERMISSION_UPDATE",
                    "ROLE_READ",
                    "ROLE_UPDATE"
            );

    private static final Set<String>
            SYSTEM_ROLES =
            Set.of(
                    "ADMIN",
                    "PACIENTE",
                    "RECEPCIONISTA",
                    "CAJERO",
                    "ENFERMERIA",
                    "MEDICO",
                    "LABORATORIO",
                    "FARMACIA"
            );

    private final PermissionRepository
            permissionRepository;

    private final RoleRepository
            roleRepository;

    private final RolePermissionRepository
            rolePermissionRepository;

    private final AuditService
            auditService;

    public RolePermissionService(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            AuditService auditService
    ) {
        this.permissionRepository =
                permissionRepository;

        this.roleRepository =
                roleRepository;

        this.rolePermissionRepository =
                rolePermissionRepository;

        this.auditService =
                auditService;
    }

    /**
     * Devuelve los permisos técnicos activos.
     *
     * El filtro por módulo es opcional.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse>
    getActivePermissions(
            String module
    ) {
        List<Permission> permissions;

        if (
                module == null ||
                        module.isBlank()
        ) {
            permissions =
                    permissionRepository
                            .findByActiveTrueOrderByModuleAscCodeAsc();

        } else {
            String normalizedModule =
                    normalizeModule(module);

            permissions =
                    permissionRepository
                            .findByModuleIgnoreCaseAndActiveTrueOrderByCodeAsc(
                                    normalizedModule
                            );
        }

        return permissions
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    /**
     * Devuelve todos los permisos activos e indica
     * cuáles están asignados al rol.
     */
    @Transactional(readOnly = true)
    public RolePermissionsResponse
    getRolePermissions(
            Long roleId
    ) {
        Role role =
                findRole(roleId);

        List<Permission> availablePermissions =
                permissionRepository
                        .findByActiveTrueOrderByModuleAscCodeAsc();

        Map<Long, RolePermission> assignments =
                new HashMap<>();

        for (
                RolePermission assignment
                : rolePermissionRepository
                .findByRole_Id(roleId)
        ) {
            assignments.put(
                    assignment
                            .getPermission()
                            .getId(),
                    assignment
            );
        }

        List<RolePermissionResponse> responses =
                availablePermissions
                        .stream()
                        .map(permission -> {
                            RolePermission assignment =
                                    assignments.get(
                                            permission.getId()
                                    );

                            boolean assigned =
                                    assignment != null &&
                                            Boolean.TRUE.equals(
                                                    assignment
                                                            .getActive()
                                            );

                            return RolePermissionResponse
                                    .builder()
                                    .permissionId(
                                            permission.getId()
                                    )
                                    .permissionCode(
                                            permission.getCode()
                                    )
                                    .permissionName(
                                            permission.getName()
                                    )
                                    .module(
                                            permission.getModule()
                                    )
                                    .description(
                                            permission.getDescription()
                                    )
                                    .assigned(assigned)
                                    .rolePermissionId(
                                            assignment != null
                                                    ? assignment.getId()
                                                    : null
                                    )
                                    .rolePermissionVersion(
                                            assignment != null
                                                    ? assignment.getVersion()
                                                    : null
                                    )
                                    .build();
                        })
                        .toList();

        int assignedCount =
                (int) responses
                        .stream()
                        .filter(response ->
                                Boolean.TRUE.equals(
                                        response.getAssigned()
                                )
                        )
                        .count();

        return RolePermissionsResponse
                .builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .roleDescription(
                        role.getDescription()
                )
                .roleActive(role.getActive())
                .systemRole(
                        isSystemRole(
                                role.getName()
                        )
                )
                .assignedPermissionCount(
                        assignedCount
                )
                .availablePermissionCount(
                        responses.size()
                )
                .permissions(responses)
                .build();
    }

    /**
     * Reemplaza la selección completa de permisos
     * activos de un rol.
     *
     * No elimina relaciones físicamente.
     */
    @Transactional
    public RolePermissionsResponse
    updateRolePermissions(
            Long roleId,
            UpdateRolePermissionsRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de permisos son obligatorios."
            );
        }

        if (request.getPermissionIds() == null) {
            throw new RuntimeException(
                    "Debe enviar la lista de permisos."
            );
        }

        Role role =
                findActiveRole(roleId);

        Set<Long> requestedPermissionIds =
                new LinkedHashSet<>(
                        request.getPermissionIds()
                );

        if (
                requestedPermissionIds.contains(null)
        ) {
            throw new RuntimeException(
                    "La lista contiene un permiso inválido."
            );
        }

        /*
         * Obtener y validar todos los permisos
         * solicitados.
         */
        Map<Long, Permission> requestedPermissions =
                new HashMap<>();

        for (
                Long permissionId
                : requestedPermissionIds
        ) {
            Permission permission =
                    permissionRepository
                            .findByIdAndActiveTrue(
                                    permissionId
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "El permiso ID "
                                                    + permissionId
                                                    + " no existe o está inactivo."
                                    )
                            );

            requestedPermissions.put(
                    permissionId,
                    permission
            );
        }

        Set<String> requestedCodes =
                requestedPermissions
                        .values()
                        .stream()
                        .map(Permission::getCode)
                        .map(code ->
                                code.trim()
                                        .toUpperCase(
                                                Locale.ROOT
                                        )
                        )
                        .collect(
                                Collectors.toSet()
                        );

        validateRequiredAdminPermissions(
                role,
                requestedCodes
        );

        LocalDateTime now =
                LocalDateTime.now();

        Map<Long, RolePermission>
                existingAssignments =
                new HashMap<>();

        for (
                RolePermission assignment
                : rolePermissionRepository
                .findByRole_Id(roleId)
        ) {
            existingAssignments.put(
                    assignment
                            .getPermission()
                            .getId(),
                    assignment
            );
        }

        List<RolePermission> assignmentsToSave =
                new ArrayList<>();

        /*
         * Crear o reactivar permisos seleccionados.
         */
        for (
                Permission permission
                : requestedPermissions.values()
        ) {
            RolePermission assignment =
                    existingAssignments.get(
                            permission.getId()
                    );

            if (assignment == null) {
                assignment =
                        RolePermission.builder()
                                .role(role)
                                .permission(permission)
                                .active(true)
                                .createdAt(now)
                                .createdBy(
                                        adminUsername
                                )
                                .updatedAt(now)
                                .updatedBy(
                                        adminUsername
                                )
                                .build();

                assignmentsToSave.add(
                        assignment
                );

            } else if (
                    !Boolean.TRUE.equals(
                            assignment.getActive()
                    )
            ) {
                assignment.setActive(true);
                assignment.setUpdatedAt(now);
                assignment.setUpdatedBy(
                        adminUsername
                );

                assignmentsToSave.add(
                        assignment
                );
            }
        }

        /*
         * Desactivar permisos omitidos.
         */
        for (
                RolePermission assignment
                : existingAssignments.values()
        ) {
            Long permissionId =
                    assignment
                            .getPermission()
                            .getId();

            if (
                    Boolean.TRUE.equals(
                            assignment.getActive()
                    ) &&
                            !requestedPermissionIds
                                    .contains(
                                            permissionId
                                    )
            ) {
                assignment.setActive(false);
                assignment.setUpdatedAt(now);
                assignment.setUpdatedBy(
                        adminUsername
                );

                assignmentsToSave.add(
                        assignment
                );
            }
        }

        try {
            if (!assignmentsToSave.isEmpty()) {
                rolePermissionRepository
                        .saveAllAndFlush(
                                assignmentsToSave
                        );
            }

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "Los permisos del rol fueron actualizados "
                            + "por otro usuario. Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_ROLE_PERMISSIONS",
                "SECURITY",
                "Permisos actualizados para el rol "
                        + role.getName()
                        + ". Total activo solicitado: "
                        + requestedPermissionIds.size()
                        + "."
        );

        RolePermissionsResponse response =
                getRolePermissions(roleId);

        response.setMessage(
                "Permisos del rol actualizados correctamente."
        );

        return response;
    }

    private Role findRole(
            Long roleId
    ) {
        if (roleId == null) {
            throw new RuntimeException(
                    "Debe indicar el rol."
            );
        }

        return roleRepository
                .findById(roleId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Rol no encontrado."
                        )
                );
    }

    private Role findActiveRole(
            Long roleId
    ) {
        if (roleId == null) {
            throw new RuntimeException(
                    "Debe indicar el rol."
            );
        }

        return roleRepository
                .findByIdAndActiveTrue(
                        roleId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "El rol no existe o está inactivo."
                        )
                );
    }

    /**
     * Impide retirar a ADMIN los permisos
     * necesarios para recuperar la administración.
     */
    private void validateRequiredAdminPermissions(
            Role role,
            Set<String> requestedCodes
    ) {
        if (
                !"ADMIN".equals(
                        normalizeRoleName(
                                role.getName()
                        )
                )
        ) {
            return;
        }

        Set<String> missingCodes =
                new LinkedHashSet<>(
                        REQUIRED_ADMIN_PERMISSIONS
                );

        missingCodes.removeAll(
                requestedCodes
        );

        if (!missingCodes.isEmpty()) {
            throw new RuntimeException(
                    "El rol ADMIN no puede perder "
                            + "los permisos obligatorios: "
                            + String.join(
                            ", ",
                            missingCodes
                    )
                            + "."
            );
        }
    }

    private String normalizeModule(
            String value
    ) {
        String cleanValue =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                "_"
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !cleanValue.matches(
                        "^[A-Z][A-Z0-9_]*$"
                )
        ) {
            throw new RuntimeException(
                    "El módulo del permiso no es válido."
            );
        }

        return cleanValue;
    }

    private boolean isSystemRole(
            String name
    ) {
        return SYSTEM_ROLES.contains(
                normalizeRoleName(name)
        );
    }

    private String normalizeRoleName(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .trim()
                .replaceAll(
                        "\\s+",
                        "_"
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private PermissionResponse
    toPermissionResponse(
            Permission permission
    ) {
        return PermissionResponse
                .builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .module(permission.getModule())
                .description(
                        permission.getDescription()
                )
                .active(permission.getActive())
                .createdAt(
                        permission
                                .getCreatedAt()
                                .toString()
                )
                .createdBy(
                        permission.getCreatedBy()
                )
                .updatedAt(
                        permission
                                .getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        permission.getUpdatedBy()
                )
                .version(
                        permission.getVersion()
                )
                .build();
    }
}