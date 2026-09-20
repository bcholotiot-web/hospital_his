package com.hospital.his.prescriptions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrescriptionItemRequest {

    /**
     * Medicamento seleccionado del catálogo.
     */
    @NotNull(
            message =
                    "Debe seleccionar un medicamento."
    )
    private Long medicationId;

    /**
     * Dosis indicada por el médico.
     */
    @NotBlank(
            message =
                    "La dosis del medicamento es obligatoria."
    )
    @Size(
            min = 1,
            max = 100,
            message =
                    "La dosis no puede exceder los 100 caracteres."
    )
    private String dosage;

    /**
     * Frecuencia de administración.
     */
    @NotBlank(
            message =
                    "La frecuencia del medicamento es obligatoria."
    )
    @Size(
            min = 1,
            max = 150,
            message =
                    "La frecuencia no puede exceder los 150 caracteres."
    )
    private String frequency;

    /**
     * Duración del tratamiento.
     */
    @NotBlank(
            message =
                    "La duración del tratamiento es obligatoria."
    )
    @Size(
            min = 1,
            max = 100,
            message =
                    "La duración no puede exceder los 100 caracteres."
    )
    private String duration;

    /**
     * Cantidad total prescrita.
     *
     * El máximo protege frente a valores
     * extremadamente altos o accidentales.
     */
    @NotNull(
            message =
                    "La cantidad prescrita es obligatoria."
    )
    @Min(
            value = 1,
            message =
                    "La cantidad prescrita debe ser mayor que cero."
    )
    @Max(
            value = 10000,
            message =
                    "La cantidad prescrita no puede exceder 10000 unidades."
    )
    private Integer prescribedQuantity;

    /**
     * Indicaciones opcionales.
     */
    @Size(
            max = 1000,
            message =
                    "Las indicaciones especiales no pueden "
                            + "exceder los 1000 caracteres."
    )
    private String specialInstructions;
}