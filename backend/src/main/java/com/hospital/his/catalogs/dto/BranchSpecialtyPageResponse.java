package com.hospital.his.catalogs.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada para el listado administrativo.
 *
 * Evita devolver directamente objetos Page de Spring,
 * manteniendo un contrato controlado con React.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSpecialtyPageResponse {

    private List<BranchSpecialtyResponse> content;

    /**
     * Página actual con índice iniciado en cero.
     */
    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}