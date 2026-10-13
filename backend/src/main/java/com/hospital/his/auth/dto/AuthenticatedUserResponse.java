package com.hospital.his.auth.dto;

import lombok.*;

import java.util.List;

/**
 * Información de la sesión autenticada.
 *
 * React utiliza esta respuesta para:
 *
 * - Mostrar el nombre del usuario.
 * - Identificar el rol.
 * - Construir el menú.
 * - Proteger rutas.
 * - Mostrar u ocultar botones.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticatedUserResponse {

    private Long userId;

    private String username;

    private String fullName;

    private Long roleId;

    private String role;

    private Long branchId;

    private String branchName;

    /**
     * Autoridades efectivas del usuario.
     *
     * Ejemplos:
     *
     * ROLE_ADMIN
     * SPECIALTY_READ
     * SPECIALTY_CREATE
     * BRANCH_READ
     */
    private List<String> authorities;
}