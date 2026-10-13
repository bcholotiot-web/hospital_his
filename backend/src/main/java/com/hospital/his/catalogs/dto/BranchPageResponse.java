package com.hospital.his.catalogs.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada del mantenimiento
 * administrativo de sucursales.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchPageResponse {

    private List<BranchResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}