package com.hospital.his.medicalagenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Período bloqueado por un evento personal
 * activo de la agenda médica.
 *
 * Este DTO se utiliza internamente para calcular
 * la disponibilidad de horarios sin ejecutar una
 * consulta por cada bloque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaBusyPeriodResponse {

    private Long eventId;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Boolean allDay;

    private String eventType;
}