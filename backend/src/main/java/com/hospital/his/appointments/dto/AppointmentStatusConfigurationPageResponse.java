package com.hospital.his.appointments.dto;

import lombok.*;

import java.util.List;

/**
 * Respuesta paginada del catálogo administrativo
 * de estados de cita.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusConfigurationPageResponse {

    private List<AppointmentStatusConfigurationResponse>
            content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;

    private Boolean empty;
}