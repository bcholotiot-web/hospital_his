package com.hospital.his.users.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada del mantenimiento de roles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePageResponse {

    private List<RoleResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}