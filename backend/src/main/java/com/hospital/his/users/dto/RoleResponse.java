package com.hospital.his.users.dto;

import lombok.*;

/**
 * Respuesta completa del catálogo de roles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private Long id;

    private String name;

    private String description;

    private Boolean active;

    /**
     * Indica si el nombre técnico del rol está
     * protegido porque es utilizado por Spring Security.
     */
    private Boolean systemRole;

    /**
     * Cantidad total de usuarios asociados.
     */
    private Long assignedUserCount;

    /**
     * Cantidad de usuarios activos asociados.
     */
    private Long activeUserCount;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}