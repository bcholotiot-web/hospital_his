package com.hospital.his.users.dto;

import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reemplaza la selección completa de permisos
 * activos de un rol.
 *
 * Comportamiento:
 *
 * - Los IDs incluidos serán creados o reactivados.
 * - Los IDs omitidos serán desactivados.
 * - No se eliminan físicamente relaciones.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRolePermissionsRequest {

    @NotNull(
            message =
                    "Debe enviar la lista de permisos."
    )
    @Builder.Default
    private Set<Long> permissionIds =
            new LinkedHashSet<>();
}