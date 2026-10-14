package com.hospital.his.medicalagenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un evento personal
 * de agenda médica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaEventResponse {

    private Long eventId;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String description;

    private String startDateTime;

    private String endDateTime;

    private Boolean allDay;

    private String eventType;

    /**
     * Nombre visible del tipo.
     */
    private String eventTypeLabel;

    /**
     * Todos los eventos personales utilizan
     * el color violeta definido en CU-16.
     */
    private String color;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}