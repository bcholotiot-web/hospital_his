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
 * Solicitud para crear una tarea personal
 * dentro de la agenda médica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicalAgendaTaskRequest {

    /**
     * Título breve de la tarea.
     */
    @NotBlank(
            message =
                    "El título de la tarea es obligatorio."
    )
    @Size(
            max = 150,
            message =
                    "El título no puede exceder los 150 caracteres."
    )
    private String title;

    /**
     * Descripción opcional.
     *
     * CU-16 permite un máximo de
     * 1000 caracteres.
     */
    @Size(
            max = 1000,
            message =
                    "La descripción no puede exceder los 1000 caracteres."
    )
    private String description;

    /**
     * Prioridad:
     *
     * BAJA
     * NORMAL
     * ALTA
     */
    @NotBlank(
            message =
                    "La prioridad es obligatoria."
    )
    private String priority;

    /**
     * Fecha y hora límite.
     */
    @NotNull(
            message =
                    "La fecha límite es obligatoria."
    )
    private LocalDateTime dueDateTime;
}