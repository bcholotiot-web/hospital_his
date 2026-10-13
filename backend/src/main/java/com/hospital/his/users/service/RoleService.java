package com.hospital.his.users.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.users.dto.CreateRoleRequest;
import com.hospital.his.users.dto.RolePageResponse;
import com.hospital.his.users.dto.RoleResponse;
import com.hospital.his.users.dto.UpdateRoleRequest;
import com.hospital.his.users.entity.Role;
import com.hospital.his.users.repository.RoleRepository;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    /**
     * Roles técnicos utilizados en SecurityConfig
     * y otras validaciones del backend.
     *
     * Su nombre no puede modificarse.
     */
    private static final Set<String>
            PROTECTED_ROLE_NAMES =
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

    private final RoleRepository
            roleRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public RoleService(
            RoleRepository roleRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.roleRepository =
                roleRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Crea un rol activo.
     *
     * Si existe una fila inactiva con el mismo
     * nombre, reactiva el registro anterior.
     */
    @Transactional
    public RoleResponse createRole(
            CreateRoleRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del rol son obligatorios."
            );
        }

        String name =
                normalizeRoleName(
                        request.getName()
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        250,
                        "La descripción del rol es obligatoria.",
                        "La descripción no puede exceder los 250 caracteres."
                );

        Role existing =
                roleRepository
                        .findByNameIgnoreCase(name)
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        if (existing != null) {
            if (
                    Boolean.TRUE.equals(
                            existing.getActive()
                    )
            ) {
                throw new RuntimeException(
                        "El rol ya se encuentra registrado."
                );
            }

            existing.setDescription(
                    description
            );
            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    adminUsername
            );

            try {
                existing =
                        roleRepository.saveAndFlush(
                                existing
                        );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "El rol fue actualizado por otro usuario. "
                                + "Recargue la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_ROLE",
                    "USERS",
                    "Rol "
                            + existing.getName()
                            + " reactivado."
            );

            return toResponse(
                    existing,
                    "Rol reactivado correctamente."
            );
        }

        Role role =
                Role.builder()
                        .name(name)
                        .description(
                                description
                        )
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

        try {
            role =
                    roleRepository.saveAndFlush(
                            role
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "El rol ya se encuentra registrado."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_ROLE",
                "USERS",
                "Rol "
                        + role.getName()
                        + " creado correctamente."
        );

        return toResponse(
                role,
                "Rol creado correctamente."
        );
    }

    /**
     * Listado administrativo paginado.
     */
    @Transactional(readOnly = true)
    public RolePageResponse getRolesPage(
            String name,
            Boolean active,
            Integer page,
            Integer size
    ) {
        int safePage =
                page != null
                        ? page
                        : 0;

        int safeSize =
                size != null
                        ? size
                        : 20;

        if (safePage < 0) {
            throw new RuntimeException(
                    "El número de página no puede ser negativo."
            );
        }

        if (
                safeSize <= 0 ||
                        safeSize > MAXIMUM_PAGE_SIZE
        ) {
            throw new RuntimeException(
                    "El tamaño de página debe estar entre 1 y 100 registros."
            );
        }

        String cleanName =
                name != null
                        ? name.trim()
                        : "";

        if (cleanName.length() > 50) {
            throw new RuntimeException(
                    "El filtro de nombre no puede exceder los 50 caracteres."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );

        Page<Role> result;

        if (
                !cleanName.isBlank() &&
                        active != null
        ) {
            result =
                    roleRepository
                            .findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
                                    cleanName,
                                    active,
                                    pageable
                            );

        } else if (!cleanName.isBlank()) {
            result =
                    roleRepository
                            .findByNameContainingIgnoreCaseOrderByNameAsc(
                                    cleanName,
                                    pageable
                            );

        } else if (active != null) {
            result =
                    roleRepository
                            .findByActiveOrderByNameAsc(
                                    active,
                                    pageable
                            );

        } else {
            result =
                    roleRepository
                            .findAllByOrderByNameAsc(
                                    pageable
                            );
        }

        return RolePageResponse.builder()
                .content(
                        result.getContent()
                                .stream()
                                .map(role ->
                                        toResponse(
                                                role,
                                                null
                                        )
                                )
                                .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(
                        result.getTotalElements()
                )
                .totalPages(
                        result.getTotalPages()
                )
                .first(result.isFirst())
                .last(result.isLast())
                .empty(result.isEmpty())
                .build();
    }

    /**
     * Devuelve todos los roles para conservar
     * compatibilidad administrativa.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository
                .findAllByOrderByNameAsc(
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(role ->
                        toResponse(
                                role,
                                null
                        )
                )
                .toList();
    }

    /**
     * Devuelve únicamente roles activos.
     *
     * Este método debe utilizarse en el formulario
     * de creación y edición de usuarios.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getActiveRoles() {
        return roleRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(role ->
                        toResponse(
                                role,
                                null
                        )
                )
                .toList();
    }

    /**
     * Busca un rol por ID.
     */
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(
            Long roleId
    ) {
        return toResponse(
                findRole(roleId),
                "Rol encontrado."
        );
    }

    /**
     * Actualiza nombre y descripción.
     *
     * Los roles técnicos protegidos no pueden
     * cambiar su nombre.
     */
    @Transactional
    public RoleResponse updateRole(
            Long roleId,
            UpdateRoleRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización son obligatorios."
            );
        }

        Role role =
                findRole(roleId);

        validateVersion(
                role,
                request.getVersion()
        );

        String newName =
                normalizeRoleName(
                        request.getName()
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        250,
                        "La descripción del rol es obligatoria.",
                        "La descripción no puede exceder los 250 caracteres."
                );

        String currentName =
                normalizeRoleName(
                        role.getName()
                );

        if (
                isProtectedRole(currentName) &&
                        !currentName.equals(newName)
        ) {
            throw new RuntimeException(
                    "El nombre del rol técnico "
                            + currentName
                            + " no puede modificarse."
            );
        }

        if (
                roleRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                newName,
                                role.getId()
                        )
        ) {
            throw new RuntimeException(
                    "El rol ya se encuentra registrado."
            );
        }

        role.setName(newName);
        role.setDescription(
                description
        );
        role.setUpdatedAt(
                LocalDateTime.now()
        );
        role.setUpdatedBy(
                adminUsername
        );

        try {
            role =
                    roleRepository.saveAndFlush(
                            role
                    );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El rol fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_ROLE",
                "USERS",
                "Rol ID "
                        + role.getId()
                        + " actualizado. Nombre: "
                        + role.getName()
                        + "."
        );

        return toResponse(
                role,
                "Rol actualizado correctamente."
        );
    }

    /**
     * Activa o desactiva lógicamente un rol.
     *
     * No permite desactivar:
     *
     * - Rol ADMIN.
     * - Rol con usuarios activos.
     */
    @Transactional
    public RoleResponse changeStatus(
            Long roleId,
            Boolean active,
            Long version,
            String adminUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado."
            );
        }

        Role role =
                findRole(roleId);

        validateVersion(
                role,
                version
        );

        if (
                Boolean.TRUE.equals(
                        role.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "El rol ya se encuentra activo."
                            : "El rol ya se encuentra inactivo."
            );
        }

        String roleName =
                normalizeRoleName(
                        role.getName()
                );

        if (
                !active &&
                        "ADMIN".equals(roleName)
        ) {
            throw new RuntimeException(
                    "El rol ADMIN no puede desactivarse."
            );
        }

        if (
                !active &&
                        userRepository
                                .existsByRole_IdAndActiveTrue(
                                        role.getId()
                                )
        ) {
            throw new RuntimeException(
                    "El rol no puede desactivarse porque "
                            + "tiene usuarios activos asignados."
            );
        }

        role.setActive(active);
        role.setUpdatedAt(
                LocalDateTime.now()
        );
        role.setUpdatedBy(
                adminUsername
        );

        try {
            role =
                    roleRepository.saveAndFlush(
                            role
                    );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El rol fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                active
                        ? "REACTIVATE_ROLE"
                        : "DEACTIVATE_ROLE",
                "USERS",
                "Rol ID "
                        + role.getId()
                        + " cambió su estado a "
                        + (
                        active
                                ? "ACTIVO"
                                : "INACTIVO"
                )
                        + "."
        );

        return toResponse(
                role,
                active
                        ? "Rol reactivado correctamente."
                        : "Rol desactivado correctamente."
        );
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

    private void validateVersion(
            Role role,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new RuntimeException(
                    "Debe indicar la versión del rol."
            );
        }

        if (
                !role.getVersion()
                        .equals(
                                requestedVersion
                        )
        ) {
            throw new RuntimeException(
                    "El rol fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }
    }

    /**
     * Normaliza el nombre utilizado por Spring Security.
     *
     * Médico -> MEDICO
     * Soporte Técnico -> SOPORTE_TECNICO
     */
    private String normalizeRoleName(
            String value
    ) {
        String cleanValue =
                normalizeRequiredText(
                        value,
                        50,
                        "El nombre del rol es obligatorio.",
                        "El nombre no puede exceder los 50 caracteres."
                );

        return Normalizer
                .normalize(
                        cleanValue,
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

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String requiredMessage,
            String maximumMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    requiredMessage
            );
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length()
                        > maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    private boolean isProtectedRole(
            String roleName
    ) {
        return PROTECTED_ROLE_NAMES.contains(
                roleName
        );
    }

    private RoleResponse toResponse(
            Role role,
            String message
    ) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(
                        role.getDescription()
                )
                .active(role.getActive())
                .systemRole(
                        isProtectedRole(
                                normalizeRoleName(
                                        role.getName()
                                )
                        )
                )
                .assignedUserCount(
                        userRepository
                                .countByRole_Id(
                                        role.getId()
                                )
                )
                .activeUserCount(
                        userRepository
                                .countByRole_IdAndActiveTrue(
                                        role.getId()
                                )
                )
                .createdAt(
                        role.getCreatedAt()
                                .toString()
                )
                .createdBy(
                        role.getCreatedBy()
                )
                .updatedAt(
                        role.getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        role.getUpdatedBy()
                )
                .version(role.getVersion())
                .message(message)
                .build();
    }
}