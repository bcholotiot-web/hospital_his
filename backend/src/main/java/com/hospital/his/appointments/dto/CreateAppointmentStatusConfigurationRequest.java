package com.hospital.his.appointments.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Request para configurar un estado técnico
 * de cita.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentStatusConfigurationRequest {

    /**
     * Debe coincidir con un valor existente
     * en AppointmentStatus.
     */
    @NotBlank(
            message =
                    "El código del estado es obligatorio."
    )
    private String code;

    @NotBlank(
            message =
                    "El nombre visible es obligatorio."
    )
    @Size(
            max = 100,
            message =
                    "El nombre visible no puede exceder los 100 caracteres."
    )
    private String displayName;

    @NotBlank(
            message =
                    "La descripción del estado es obligatoria."
    )
    @Size(
            max = 500,
            message =
                    "La descripción no puede exceder los 500 caracteres."
    )
    private String description;

    @NotNull(
            message =
                    "El orden visual es obligatorio."
    )
    @Min(
            value = 0,
            message =
                    "El orden visual no puede ser negativo."
    )
    @Max(
            value = 1000,
            message =
                    "El orden visual no puede exceder 1000."
    )
    private Integer displayOrder;

    @NotBlank(
            message =
                    "El color del estado es obligatorio."
    )
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message =
                    "El color debe utilizar el formato hexadecimal #RRGGBB."
    )
    private String color;

    @NotNull(
            message =
                    "Debe indicar si el estado es visible para el paciente."
    )
    private Boolean patientVisible;

    @NotNull(
            message =
                    "Debe indicar si el estado es visible para el personal."
    )
    private Boolean staffVisible;
}