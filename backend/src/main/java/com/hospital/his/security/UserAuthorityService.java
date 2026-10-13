package com.hospital.his.security;

import com.hospital.his.users.entity.RolePermission;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.RolePermissionRepository;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Construye las autoridades efectivas de un usuario.
 *
 * El JWT identifica al usuario, pero los permisos
 * actuales se consultan desde PostgreSQL en cada
 * petición autenticada.
 *
 * Esto permite aplicar inmediatamente:
 *
 * - Desactivación del usuario.
 * - Desactivación del rol.
 * - Asignación de un permiso.
 * - Retiro de un permiso.
 * - Desactivación del permiso.
 */
@Service
public class UserAuthorityService {

    private final UserRepository
            userRepository;

    private final RolePermissionRepository
            rolePermissionRepository;

    public UserAuthorityService(
            UserRepository userRepository,
            RolePermissionRepository
                    rolePermissionRepository
    ) {
        this.userRepository =
                userRepository;

        this.rolePermissionRepository =
                rolePermissionRepository;
    }

    /**
     * Recupera el rol y los permisos activos
     * del usuario autenticado.
     *
     * Ejemplo del resultado:
     *
     * ROLE_ADMIN
     * SPECIALTY_READ
     * SPECIALTY_CREATE
     * MEDICATION_READ
     */
    @Transactional(readOnly = true)
    public List<SimpleGrantedAuthority>
    getAuthorities(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario autenticado no encontrado."
                                )
                        );

        validateUser(user);

        String normalizedRole =
                normalizeRole(
                        user.getRole().getName()
                );

        if (normalizedRole.isBlank()) {
            throw new RuntimeException(
                    "El usuario no tiene un rol válido."
            );
        }

        /*
         * LinkedHashSet evita autoridades duplicadas
         * y conserva el orden de inserción.
         */
        Set<String> authorityNames =
                new LinkedHashSet<>();

        /*
         * Mantener compatibilidad con las reglas:
         *
         * hasRole("ADMIN")
         * hasRole("MEDICO")
         * hasRole("FARMACIA")
         */
        authorityNames.add(
                "ROLE_" + normalizedRole
        );

        List<RolePermission> rolePermissions =
                rolePermissionRepository
                        .findActivePermissionsByRoleId(
                                user.getRole().getId()
                        );

        /*
         * Agregar permisos granulares.
         */
        for (
                RolePermission rolePermission
                : rolePermissions
        ) {
            String permissionCode =
                    rolePermission
                            .getPermission()
                            .getCode();

            if (
                    permissionCode == null ||
                            permissionCode.isBlank()
            ) {
                continue;
            }

            authorityNames.add(
                    permissionCode
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );
        }

        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        for (
                String authorityName
                : authorityNames
        ) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            authorityName
                    )
            );
        }

        return authorities;
    }

    /**
     * Comprueba que el usuario y su rol continúen
     * habilitados después de emitir el JWT.
     */
    private void validateUser(
            User user
    ) {
        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario se encuentra inactiva."
            );
        }

        if (user.getRole() == null) {
            throw new RuntimeException(
                    "El usuario no tiene un rol asignado."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        user.getRole().getActive()
                )
        ) {
            throw new RuntimeException(
                    "El rol del usuario se encuentra inactivo."
            );
        }

        /*
         * Conservar la validación de bloqueo temporal
         * si la entidad User posee lockedUntil.
         *
         * Si tu entidad User no tiene este campo,
         * elimina únicamente este bloque.
         */
        if (
                user.getLockedUntil() != null &&
                        user.getLockedUntil()
                                .isAfter(
                                        LocalDateTime.now()
                                )
        ) {
            throw new RuntimeException(
                    "La cuenta se encuentra temporalmente bloqueada."
            );
        }
    }

    /**
     * Normaliza nombres de rol.
     *
     * Ejemplos:
     *
     * Médico -> MEDICO
     * Administrador -> ADMIN
     */
    private String normalizeRole(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                                value.trim(),
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}",
                                ""
                        )
                        .replaceAll(
                                "\\s+",
                                "_"
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        /*
         * Compatibilidad por si la base contiene
         * nombres visibles en lugar del código.
         */
        return switch (normalized) {
            case "ADMINISTRADOR" ->
                    "ADMIN";

            case "MEDICO",
                 "DOCTOR" ->
                    "MEDICO";

            default ->
                    normalized;
        };
    }
}