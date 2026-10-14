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
 * Solicitud para crear un evento personal
 * dentro de la agenda del médico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicalAgendaEventRequest {

    /**
     * Título visible en el calendario.
     */
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

    /**
     * Descripción opcional.
     */
    @Size(
            max = 500,
            message =
                    "La descripción no puede exceder los 500 caracteres."
    )
    private String description;

    /**
     * Para eventos con horario contiene
     * fecha y hora.
     *
     * Para todo el día, el servicio utiliza
     * únicamente la parte de fecha.
     */
    @NotNull(
            message =
                    "La fecha de inicio es obligatoria."
    )
    private LocalDateTime startDateTime;

    /**
     * Debe ser posterior a la fecha de inicio.
     */
    @NotNull(
            message =
                    "La fecha de fin es obligatoria."
    )
    private LocalDateTime endDateTime;

    /**
     * Valores admitidos:
     *
     * REUNION
     * DESCANSO
     * CAPACITACION
     * PERSONAL
     * OTRO
     */
    @NotBlank(
            message =
                    "El tipo de evento es obligatorio."
    )
    private String eventType;

    /**
     * true:
     * el evento ocupa fechas completas.
     *
     * false:
     * se conservan las horas ingresadas.
     */
    @Builder.Default
    private Boolean allDay = false;
}