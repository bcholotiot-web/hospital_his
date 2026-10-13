package com.hospital.his.catalogs.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada para administración
 * de especialidades.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyPageResponse {

    private List<SpecialtyResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}