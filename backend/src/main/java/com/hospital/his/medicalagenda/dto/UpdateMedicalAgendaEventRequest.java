package com.hospital.his.medicalagenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud para actualizar un evento
 * perteneciente al médico autenticado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMedicalAgendaEventRequest {

    @NotBlank(
            message =
                    "El título del evento es obligatorio."
    )
    @Size(
            max = 150,
            message =
                    "El título no puede exceder los 150 caracteres."
    )
    private String title;

    @Size(
            max = 500,
            message =
                    "La descripción no puede exceder los 500 caracteres."
    )
    private String description;

    @NotNull(
            message =
                    "La fecha de inicio es obligatoria."
    )
    private LocalDateTime startDateTime;

    @NotNull(
            message =
                    "La fecha de fin es obligatoria."
    )
    private LocalDateTime endDateTime;

    @NotBlank(
            message =
                    "El tipo de evento es obligatorio."
    )
    private String eventType;

    @NotNull(
            message =
                    "Debe indicar si el evento ocupa todo el día."
    )
    private Boolean allDay;

    /**
     * Versión actual del evento.
     *
     * Evita que dos pestañas sobrescriban
     * cambios simultáneos.
     */
    @NotNull(
            message =
                    "La versión del evento es obligatoria."
    )
    private Long version;
}