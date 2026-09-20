package com.hospital.his.prescriptions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrescriptionRequest {

    /**
     * Cita asociada con la consulta médica.
     *
     * Desde la cita el backend obtiene:
     * - Consulta.
     * - Médico.
     * - Paciente.
     * - Sucursal.
     */
    @NotNull(
            message =
                    "Debe indicar la cita médica."
    )
    private Long appointmentId;

    /**
     * Medicamentos prescritos.
     */
    @NotEmpty(
            message =
                    "Debe agregar al menos un medicamento a la receta."
    )
    @Size(
            max = 30,
            message =
                    "No puede agregar más de 30 medicamentos por receta."
    )
    private List<
            @Valid
            @NotNull(
                    message =
                            "La receta contiene un medicamento inválido."
            )
                    CreatePrescriptionItemRequest
            > items;

    /**
     * Observaciones generales.
     */
    @Size(
            max = 1000,
            message =
                    "Las observaciones no pueden exceder los 1000 caracteres."
    )
    private String notes;
}