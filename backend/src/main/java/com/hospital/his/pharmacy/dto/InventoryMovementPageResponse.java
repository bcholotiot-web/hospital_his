package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada de movimientos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementPageResponse {

    private List<InventoryMovementResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}