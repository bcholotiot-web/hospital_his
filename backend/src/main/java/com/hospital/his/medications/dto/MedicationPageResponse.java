package com.hospital.his.medications.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada del mantenimiento
 * administrativo de medicamentos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationPageResponse {

    private List<MedicationResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}