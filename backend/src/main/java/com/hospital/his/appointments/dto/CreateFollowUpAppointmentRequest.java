package com.hospital.his.appointments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFollowUpAppointmentRequest {

    /**
     * Consulta médica que origina el seguimiento.
     */
    @NotNull(message = "Debe indicar la consulta médica de origen.")
    private Long parentConsultationId;

    /**
     * Valores permitidos:
     *
     * MONITOREO_TRATAMIENTO
     * REVISION_RESULTADOS_LABORATORIO
     */
    @NotBlank(message = "Debe seleccionar el tipo de seguimiento.")
    private String followUpType;

    /**
     * Fecha y hora elegidas en formato ISO.
     *
     * Ejemplo:
     * 2026-09-28T09:00:00
     */
    @NotBlank(message = "Debe seleccionar la fecha y hora del seguimiento.")
    private String appointmentDateTime;

    /**
     * Motivo concreto del seguimiento.
     */
    @NotBlank(message = "El motivo del seguimiento es obligatorio.")
    @Size(min = 10, max = 2000, message = "El motivo debe contener entre 10 y 2000 caracteres.")
    private String reason;

    /**
     * Prioridad de la nueva cita.
     *
     * Se validará contra AppointmentPriority.
     */
    @NotBlank(message = "Debe seleccionar la prioridad del seguimiento.")
    private String priority;

    /**
     * UUID generado por el frontend.
     *
     * Evita crear dos citas por doble clic o reintento.
     */
    @NotNull(message = "La llave de idempotencia es obligatoria.")
    private UUID idempotencyKey;
}