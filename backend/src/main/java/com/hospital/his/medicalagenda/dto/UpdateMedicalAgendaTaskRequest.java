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
 * Solicitud para actualizar una tarea
 * perteneciente al médico autenticado.
 *
 * El estado completado se modifica mediante
 * un endpoint separado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMedicalAgendaTaskRequest {

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

    @Size(
            max = 1000,
            message =
                    "La descripción no puede exceder los 1000 caracteres."
    )
    private String description;

    @NotBlank(
            message =
                    "La prioridad es obligatoria."
    )
    private String priority;

    @NotNull(
            message =
                    "La fecha límite es obligatoria."
    )
    private LocalDateTime dueDateTime;

    /**
     * Versión utilizada para evitar que dos
     * pestañas sobrescriban cambios.
     */
    @NotNull(
            message =
                    "La versión de la tarea es obligatoria."
    )
    private Long version;
}