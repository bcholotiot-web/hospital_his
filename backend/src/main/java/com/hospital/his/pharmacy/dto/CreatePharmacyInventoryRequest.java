package com.hospital.his.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePharmacyInventoryRequest {

    /**
     * Medicamento que se agregará al inventario.
     *
     * La sucursal no se recibe en el request.
     * Se obtiene desde el usuario autenticado.
     */
    @NotNull(
            message =
                    "Debe seleccionar un medicamento."
    )
    private Long medicationId;

    /**
     * Cantidad inicial disponible.
     */
    @NotNull(
            message =
                    "Debe indicar el stock inicial."
    )
    @Min(
            value = 0,
            message =
                    "El stock inicial no puede ser negativo."
    )
    private Integer currentStock;

    /**
     * Cantidad mínima configurada para generar
     * alertas de reabastecimiento.
     */
    @NotNull(
            message =
                    "Debe indicar el stock mínimo."
    )
    @Min(
            value = 0,
            message =
                    "El stock mínimo no puede ser negativo."
    )
    private Integer minimumStock;
}