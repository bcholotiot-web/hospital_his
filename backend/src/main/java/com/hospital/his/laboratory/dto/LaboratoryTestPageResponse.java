package com.hospital.his.laboratory.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada del catálogo administrativo
 * de exámenes de laboratorio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryTestPageResponse {

    private List<LaboratoryTestResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}