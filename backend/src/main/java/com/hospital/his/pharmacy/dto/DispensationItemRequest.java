package com.hospital.his.pharmacy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

//Request de cada medicamento a despachar
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispensationItemRequest {

    /**
     * Ítem original de la receta.
     */
    @NotNull(
            message =
                    "Debe indicar el medicamento de la receta."
    )
    private Long prescriptionItemId;

    /**
     * Cantidad que será entregada.
     */
    @NotNull(
            message =
                    "Debe indicar la cantidad a despachar."
    )
    @Min(
            value = 1,
            message =
                    "La cantidad a despachar debe ser mayor que cero."
    )
    @Max(
            value = 10000,
            message =
                    "La cantidad a despachar no puede exceder 10000 unidades."
    )
    private Integer dispensedQuantity;

    /**
     * Indica si se sustituirá el medicamento original.
     */
    @NotNull(
            message =
                    "Debe indicar si el medicamento será sustituido."
    )
    private Boolean substituted;

    /**
     * Medicamento que será entregado cuando exista
     * una sustitución.
     */
    private Long alternativeMedicationId;

    /**
     * Justificación obligatoria cuando existe sustitución.
     */
    @Size(
            max = 1000,
            message =
                    "La justificación de sustitución no puede "
                            + "exceder los 1000 caracteres."
    )
    private String substitutionReason;

    /**
     * Autorización adicional para medicamentos controlados.
     */
    @Size(
            max = 200,
            message =
                    "La autorización no puede exceder "
                            + "los 200 caracteres."
    )
    private String controlAuthorization;
}