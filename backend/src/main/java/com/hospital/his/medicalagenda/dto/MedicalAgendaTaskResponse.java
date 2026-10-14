package com.hospital.his.medicalagenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de una tarea personal
 * de la agenda médica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaTaskResponse {

    private Long taskId;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String description;

    /**
     * Código técnico:
     *
     * BAJA
     * NORMAL
     * ALTA
     */
    private String priority;

    /**
     * Nombre visible:
     *
     * Baja
     * Normal
     * Alta
     */
    private String priorityLabel;

    /**
     * Color sugerido para el frontend.
     *
     * BAJA:
     * #3B82F6
     *
     * NORMAL:
     * #F59E0B
     *
     * ALTA:
     * #EF4444
     */
    private String priorityColor;

    private String dueDateTime;

    private Boolean completed;

    private String completedAt;

    private Boolean overdue;

    private Boolean active;

    private String createdAt;

    private String createdBy;

    private String updatedAt;

    private String updatedBy;

    private Long version;

    private String message;
}